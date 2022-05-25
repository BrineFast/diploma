package com.diploma.slepov.custom.processor

import android.content.Context
import android.util.Base64
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.diploma.slepov.custom.view.objectdetection.DetectedInfo
import com.google.android.gms.tasks.Tasks
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** Класс поиска изображений **/
class SearchEngine(context: Context) {

    private val searchQueue: RequestQueue = Volley.newRequestQueue(context)
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    fun search(
            detectedObject: DetectedInfo,
            listener: (detectedObject: DetectedInfo, productList: List<Product>) -> Unit
    ) {
        Tasks.call<JsonObjectRequest>(executor, Callable { createRequest(detectedObject, searchQueue, listener) })
            .addOnSuccessListener { productRequest -> searchQueue.add(productRequest) }
    }

    fun shutdown() {
        searchQueue.cancelAll(TAG)
        executor.shutdown()
    }

    /** Отправка полученных изображений на сервер и обработка ответа **/
    companion object {
        private const val TAG = "SearchEngine"

        const val VISION_API_URL =
            "https://vision.googleapis.com/v1"
        const val VISION_API_KEY = "AIzaSyAgocknz8wGHGOwyWqDDfKOUckggXx_bQ4"

        @Throws(Exception::class)
        private fun createRequest(
                searchingObject: DetectedInfo,
                searchQueue: RequestQueue,
                listener: (
                        detectedObject: DetectedInfo,
                        productList: List<Product>) -> Unit
        ): JsonObjectRequest {

            fun retrieveImages(results: JSONObject) {
                val productList = ArrayList<Product>()
                val requestList = ArrayList<JsonObjectRequest>()
                val description = results.get("webEntities") as JSONArray
                val url = results.get("visuallySimilarImages") as JSONArray
                for (index in 0 until (description).length()) {
                    val title = description.getJSONObject(index).get("description") as String
                    val image = url.getJSONObject(index).get("url") as String
                    val subtitle = "Product"
                    productList.add(Product(image, title, subtitle))
                }
                requestList.forEach({request -> searchQueue.add(request)})
                Thread.sleep(2500)
                listener.invoke(searchingObject, productList)
            }

            val objectImageData: String = Base64.encodeToString(searchingObject.imageData, Base64.DEFAULT)

            val requestJson = """
            {
              "requests": [
                {
                  "image": {
                    "content": """".trimIndent() + objectImageData + """"
                  },
                  "features": [
                    {
                      "type": "WEB_DETECTION",
                      "maxResults": 4
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

            return (object :
                JsonObjectRequest(
                    Method.POST,
                    "$VISION_API_URL/images:annotate?key=$VISION_API_KEY",
                    JSONObject(requestJson),
                    { response ->
                        val results = ((response.get("responses") as JSONArray)
                            .getJSONObject(0)
                            .get("webDetection") as JSONObject)
                        retrieveImages(results)
                    },
                    { error -> error }
                ) {
                override fun getBodyContentType() = "application/json"
            }.apply {
                setShouldCache(false)
            })
        }
    }
}
