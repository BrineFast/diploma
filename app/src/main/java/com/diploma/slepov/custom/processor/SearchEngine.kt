package com.diploma.slepov.custom.processor

import android.content.Context
import android.util.Base64
import android.util.Log
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.diploma.slepov.custom.view.objectdetection.DetectedObjectInfo
import com.diploma.slepov.custom.view.productsearch.Product
import com.google.android.gms.tasks.Tasks
import com.google.gson.Gson
import org.json.JSONArray
import org.json.JSONObject
import java.lang.reflect.Method
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


/** A fake search engine to help simulate the complete work flow.  */
class SearchEngine(context: Context) {

    private val searchRequestQueue: RequestQueue = Volley.newRequestQueue(context)
    private val requestCreationExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    fun search(
        detectedObject: DetectedObjectInfo,
        listener: (detectedObject: DetectedObjectInfo, productList: List<Product>) -> Unit
    ) {
        // Crops the object image out of the full image is expensive, so do it off the UI thread.
        Tasks.call<JsonObjectRequest>(requestCreationExecutor, Callable { createRequest(detectedObject, searchRequestQueue, listener) })
            .addOnSuccessListener { productRequest -> searchRequestQueue.add(productRequest) }
    }

    fun shutdown() {
        searchRequestQueue.cancelAll(TAG)
        requestCreationExecutor.shutdown()
    }

    companion object {
        private const val TAG = "SearchEngine"

        const val VISION_API_URL =
            "https://us-central1-odml-codelabs.cloudfunctions.net/productSearch"
        const val VISION_API_KEY = ""
        const val VISION_API_PROJECT_ID = "odml-codelabs"
        const val VISION_API_LOCATION_ID = "us-east1"
        const val VISION_API_PRODUCT_SET_ID = "product_set0"

        @Throws(Exception::class)
        private fun createRequest(
            searchingObject: DetectedObjectInfo,
            searchRequestQueue: RequestQueue,
            listener: (
                detectedObject: DetectedObjectInfo,
                productList: List<Product>) -> Unit
        ): JsonObjectRequest {

            fun retrieveImages(results: JSONArray) {
                val productList = ArrayList<Product>()
                val requestList = ArrayList<JsonObjectRequest>()
                for (index in 0 until (results).length()) {
                    val result = results.getJSONObject(index)
                    val item = (result.get("product") as JSONObject)
                    val imageId = result.get("image").toString()

                    val title = (item.get("displayName")).toString()
                    val subtitle = (item.get("productCategory")).toString()

                    val image_response = object : JsonObjectRequest(
                            Method.GET,
                            "$VISION_API_URL/${imageId}?key=$VISION_API_KEY",
                            null,
                            { response ->
                                val image = (response.get("uri")).toString()
                                    .replace("gs://", "https://storage.googleapis.com/")

                                productList.add(Product(image, title, subtitle))
                            },
                            { error -> error }
                        ) {
                        override fun getBodyContentType(): String {
                            return "application/json; charset=utf-8"
                        }
                    }
                    requestList.add(image_response)
                }
                requestList.forEach({request -> searchRequestQueue.add(request)})
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
                      "type": "PRODUCT_SEARCH",
                      "maxResults": 4
                    }
                  ],
                  "imageContext": {
                    "productSearchParams": {
                      "productSet": "projects/${VISION_API_PROJECT_ID}/locations/${VISION_API_LOCATION_ID}/productSets/${VISION_API_PRODUCT_SET_ID}",
                      "productCategories": [
                           "apparel-v2"
                         ]
                    }
                  }
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
                        val results = (((response.get("responses") as JSONArray)
                            .getJSONObject(0)
                            .get("productSearchResults") as JSONObject)
                            .get("results") as JSONArray)
                        retrieveImages(results)
                    },
                    // Return the error
                    { error -> error }
                ) {
                override fun getBodyContentType() = "application/json"
            }.apply {
                setShouldCache(false)
            })
        }
    }
}
