package com.diploma.slepov.custom.view.productsearch

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.diploma.slepov.custom.R
import com.diploma.slepov.custom.processor.ImageRetriever
import com.diploma.slepov.custom.processor.Product
import com.diploma.slepov.custom.view.productsearch.ProductRepresentation.ProductView

/** Класс для репрезентации полученных из поиска объектов **/
class ProductRepresentation(private val productList: List<Product>) : Adapter<ProductView>() {

    /** Вспомогательный класс для резепрезентации изображений **/
    class ProductView private constructor(view: View) : RecyclerView.ViewHolder(view) {

        private val imageView: ImageView = view.findViewById(R.id.product_image)
        private val titleView: TextView = view.findViewById(R.id.product_title)
        private val subtitleView: TextView = view.findViewById(R.id.product_subtitle)
        private val imageSize: Int = view.resources.getDimensionPixelOffset(R.dimen.product_item_image_size)

        fun bindProduct(product: Product) {
            imageView.setImageDrawable(null)
            if (!TextUtils.isEmpty(product.imageUrl)) {
                ImageRetriever(imageView, imageSize).execute(product.imageUrl)
            } else {
                imageView.setImageResource(0)
            }
            titleView.text = product.title
            subtitleView.text = product.subtitle
        }

        companion object {
            fun create(parent: ViewGroup) =
                ProductView(LayoutInflater.from(parent.context).inflate(R.layout.product_item, parent, false))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductView =
        ProductView.create(parent)

    override fun onBindViewHolder(holder: ProductView, position: Int) {
        holder.bindProduct(productList[position])
    }

    override fun getItemCount(): Int = productList.size
}
