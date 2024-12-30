package com.programminghut.realtime_object

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Build
import android.util.Log
import android.util.Size
import android.widget.Toast
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.nnapi.NnApiDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.TensorProcessor
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.common.ops.DequantizeOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.common.ops.QuantizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.metadata.MetadataExtractor
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*
import kotlin.Comparator

class Yolov5TFLiteDetector {

    private val INPNUT_SIZE = Size(320, 320)
    private val OUTPUT_SIZE = intArrayOf(1, 6300, 7)
    private val IS_INT8 = false
    private val DETECT_THRESHOLD = 0.25f
    private val IOU_THRESHOLD = 0.45f
    private val IOU_CLASS_DUPLICATED_THRESHOLD = 0.7f

    private val LABEL_FILE = "labels.txt"

    private var BITMAP_HEIGHT = 0
    private var BITMAP_WIDTH = 0
    private lateinit var MODEL_FILE: String

    private lateinit var tflite: Interpreter
    private lateinit var associatedAxisLabels: List<String>
    private val options = Interpreter.Options()

    fun getModelFile(): String {
        return MODEL_FILE
    }

    fun setModelFile(modelFile: String) {
        MODEL_FILE = modelFile
        Log.d(">>> ", "MODEL NAME SET --- $MODEL_FILE, $modelFile")
    }

    fun getLabelFile(): String {
        return LABEL_FILE
    }

    fun getInputSize(): Size {
        return INPNUT_SIZE
    }

    fun getOutputSize(): IntArray {
        return OUTPUT_SIZE
    }

    /**
     * Initialize the model, pre-load delegates with addNNApiDelegate() and addGPUDelegate().
     *
     * @param activity
     */
    fun initialModel(activity: Context) {
        try {
            Log.d(">>> ", "loading model --- $MODEL_FILE")
            val tfliteModel: ByteBuffer = FileUtil.loadMappedFile(activity, MODEL_FILE)
            tflite = Interpreter(tfliteModel, options)
            Log.i("tfliteSupport", "Success reading model: $MODEL_FILE")

            associatedAxisLabels = FileUtil.loadLabels(activity, LABEL_FILE)
            Log.i("tfliteSupport", "Success reading label: $LABEL_FILE")

        } catch (e: IOException) {
            Log.e("tfliteSupport", "Error reading model or label: ", e)
            Toast.makeText(activity, "load model error: " + e.message, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Detection process
     *
     * @param bitmap
     * @return
     */
    fun detect(bitmap: Bitmap): ArrayList<Recognition> {
        BITMAP_HEIGHT = bitmap.height
        BITMAP_WIDTH = bitmap.width

        // yolov5s-tflite input is: [1, 320, 320, 3], resize and normalize the camera frame image
        var yolov5sTfliteInput: TensorImage
        val imageProcessor: ImageProcessor

        if (IS_INT8) {
            imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(INPNUT_SIZE.height, INPNUT_SIZE.width, ResizeOp.ResizeMethod.BILINEAR))
                .add(NormalizeOp(0f, 255f))
                .add(QuantizeOp(0.003921568859368563f, 0F))
                .add(CastOp(DataType.UINT8))
                .build()
            yolov5sTfliteInput = TensorImage(DataType.UINT8)
        } else {
            imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(INPNUT_SIZE.height, INPNUT_SIZE.width, ResizeOp.ResizeMethod.BILINEAR))
                .add(NormalizeOp(0f, 255f))
                .build()
            yolov5sTfliteInput = TensorImage(DataType.FLOAT32)
        }

        yolov5sTfliteInput.load(bitmap)
        yolov5sTfliteInput = imageProcessor.process(yolov5sTfliteInput)

        var probabilityBuffer: TensorBuffer
        if (IS_INT8) {
            probabilityBuffer = TensorBuffer.createFixedSize(OUTPUT_SIZE, DataType.UINT8)
        } else {
            probabilityBuffer = TensorBuffer.createFixedSize(OUTPUT_SIZE, DataType.FLOAT32)
        }

        if (null != tflite) {
            Log.d(">>> ", "${yolov5sTfliteInput.tensorBuffer.flatSize} ${probabilityBuffer.flatSize}")
            tflite.run(yolov5sTfliteInput.buffer, probabilityBuffer.buffer)
        }

        if (IS_INT8) {
            val tensorProcessor = TensorProcessor.Builder()
                .add(DequantizeOp(0.006305381190031767f, 5F))
                .build()
            probabilityBuffer = tensorProcessor.process(probabilityBuffer)
        }

        val recognitionArray = probabilityBuffer.floatArray
        val allRecognitions = ArrayList<Recognition>()

        for (i in 0 until OUTPUT_SIZE[1]) {
            val gridStride = i * OUTPUT_SIZE[2]
            val x = recognitionArray[0 + gridStride] * BITMAP_WIDTH
            val y = recognitionArray[1 + gridStride] * BITMAP_HEIGHT
            val w = recognitionArray[2 + gridStride] * BITMAP_WIDTH
            val h = recognitionArray[3 + gridStride] * BITMAP_HEIGHT
            val xmin = maxOf(0, (x - w / 2).toInt())
            val ymin = maxOf(0, (y - h / 2).toInt())
            val xmax = minOf(BITMAP_WIDTH, (x + w / 2).toInt())
            val ymax = minOf(BITMAP_HEIGHT, (y + h / 2).toInt())
            val confidence = recognitionArray[4 + gridStride]
            val classScores = Arrays.copyOfRange(recognitionArray, 5 + gridStride, OUTPUT_SIZE[2] + gridStride)
            var labelId = 0
            var maxLabelScores = 0f

            for (j in classScores.indices) {
                if (classScores[j] > maxLabelScores) {
                    maxLabelScores = classScores[j]
                    labelId = j
                }
            }

            val r = Recognition(
                labelId,
                "",
                maxLabelScores,
                confidence,
                RectF(xmin.toFloat(), ymin.toFloat(), xmax.toFloat(), ymax.toFloat())
            )
            allRecognitions.add(r)
        }

        val nmsRecognitions = nms(allRecognitions)
        val nmsFilterBoxDuplicationRecognitions = nmsAllClass(nmsRecognitions)

        for (recognition in nmsFilterBoxDuplicationRecognitions) {
            val labelId = recognition.labelId
            val labelName = associatedAxisLabels[labelId]
            recognition.labelName = labelName
        }

        return nmsFilterBoxDuplicationRecognitions
    }

    protected fun nms(allRecognitions: ArrayList<Recognition>): ArrayList<Recognition> {
        val nmsRecognitions = ArrayList<Recognition>()

        for (i in 0 until OUTPUT_SIZE[2] - 5) {
            val pq = PriorityQueue(
                6300,
                Comparator { l: Recognition, r: Recognition ->
                    java.lang.Float.compare(r.confidence, l.confidence)
                }
            )

            for (j in allRecognitions.indices) {
                if (allRecognitions[j].labelId == i && allRecognitions[j].confidence > DETECT_THRESHOLD) {
                    pq.add(allRecognitions[j])
                }
            }

            while (pq.size > 0) {
                val a = Array<Recognition>(pq.size) { Recognition(0, "", 0f, 0f, RectF()) }
                val detections = pq.toArray(a)
                val max = detections[0]
                nmsRecognitions.add(max)
                pq.clear()

                for (k in 1 until detections.size) {
                    val detection = detections[k]
                    if (boxIou(max.location, detection.location) < IOU_THRESHOLD) {
                        pq.add(detection)
                    }
                }
            }
        }
        return nmsRecognitions
    }

    protected fun nmsAllClass(allRecognitions: ArrayList<Recognition>): ArrayList<Recognition> {
        val nmsRecognitions = ArrayList<Recognition>()

        val pq = PriorityQueue(
            100,
            Comparator { l: Recognition, r: Recognition ->
                java.lang.Float.compare(r.confidence, l.confidence)
            }
        )

        for (j in allRecognitions.indices) {
            if (allRecognitions[j].confidence > DETECT_THRESHOLD) {
                pq.add(allRecognitions[j])
            }
        }

        while (pq.size > 0) {
            val a = Array<Recognition>(pq.size) { Recognition(0, "", 0f, 0f, RectF()) }
            val detections = pq.toArray(a)
            val max = detections[0]
            nmsRecognitions.add(max)
            pq.clear()

            for (k in 1 until detections.size) {
                val detection = detections[k]
                if (boxIou(max.location, detection.location) < IOU_CLASS_DUPLICATED_THRESHOLD) {
                    pq.add(detection)
                }
            }
        }
        return nmsRecognitions
    }

    protected fun boxIou(a: RectF, b: RectF): Float {
        val intersection = boxIntersection(a, b)
        val union = boxUnion(a, b)
        return if (union <= 0) 1f else intersection / union
    }

    protected fun boxIntersection(a: RectF, b: RectF): Float {
        val maxLeft = if (a.left > b.left) a.left else b.left
        val maxTop = if (a.top > b.top) a.top else b.top
        val minRight = if (a.right < b.right) a.right else b.right
        val minBottom = if (a.bottom < b.bottom) a.bottom else b.bottom
        val w = minRight - maxLeft
        val h = minBottom - maxTop
        return if (w < 0 || h < 0) 0f else w * h
    }

    protected fun boxUnion(a: RectF, b: RectF): Float {
        val i = boxIntersection(a, b)
        val u = (a.right - a.left) * (a.bottom - a.top) + (b.right - b.left) * (b.bottom - b.top) - i
        return u
    }

    /**
     * Add NNapi delegate
     */
    fun addNNApiDelegate() {
        val nnApiDelegate: NnApiDelegate?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            nnApiDelegate = NnApiDelegate()
            options.addDelegate(nnApiDelegate)
            Log.i("tfliteSupport", "using nnapi delegate.")
        }
    }

    /**
     * Add GPU delegate
     */
    fun addGPUDelegate() {
        val compatibilityList = CompatibilityList()
        if (compatibilityList.isDelegateSupportedOnThisDevice()) {
            val delegateOptions = compatibilityList.getBestOptionsForThisDevice()
            val gpuDelegate = GpuDelegate(delegateOptions)
            options.addDelegate(gpuDelegate)
            Log.i("tfliteSupport", "using gpu delegate.")
        } else {
            addThread(4)
        }
    }

    /**
     * Add number of threads
     *
     * @param thread
     */
    fun addThread(thread: Int) {
        options.setNumThreads(thread)
    }
}