package com.diploma.slepov.custom.view.productsearch

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.diploma.slepov.custom.R
import com.diploma.slepov.custom.processor.ImageRetriever
import com.diploma.slepov.custom.processor.Product

/** Класс, содержащий логику представления карточки найденных объектов при статическом режиме детектирования **/
class ProductCardPreview(
    private val objectList: List<SearchedObject>,
    private val clickCoordinator: (searchedObject: SearchedObject) -> Any
) : RecyclerView.Adapter<ProductCardPreview.CardView>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardView {
        return CardView(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.products_card, parent, false)
        )
    }

    override fun onBindViewHolder(holder: CardView, position: Int) {
        val searchedObject = objectList[position]
        holder.bindProducts(searchedObject.productList)
        holder.itemView.setOnClickListener { clickCoordinator.invoke(searchedObject) }
    }

    override fun getItemCount(): Int = objectList.size

    /** Вспомогательный класс для отображения карточек продуктов **/
    class CardView internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val imageView: ImageView = itemView.findViewById(R.id.card_image)
        private val titleView: TextView = itemView.findViewById(R.id.card_title)
        private val subtitleView: TextView = itemView.findViewById(R.id.card_subtitle)
        private val imageSize: Int = itemView.resources.getDimensionPixelOffset(R.dimen.preview_card_image_size)

        internal fun bindProducts(products: List<Product>) {
            if (products.isEmpty()) {
                imageView.visibility = View.GONE
                titleView.setText(R.string.static_image_card_no_result_title)
                subtitleView.setText(R.string.static_image_card_no_result_subtitle)
            } else {
                val topProduct = products[0]
                imageView.visibility = View.VISIBLE
                imageView.setImageDrawable(null)
                if (!TextUtils.isEmpty(topProduct.imageUrl)) {
                    ImageRetriever(imageView, imageSize).execute(topProduct.imageUrl)
                } else {
                    imageView.setImageResource(0)
                }
                titleView.text = topProduct.title
                subtitleView.text = itemView
                    .resources
                    .getString(R.string.static_image_preview_card_subtitle, products.size - 1)
            }
        }
    }
}
