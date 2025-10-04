package com.st10321779.rootedwealth.ui.rewards

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.st10321779.rootedwealth.databinding.ItemRewardsCouponBinding

class CouponAdapter(
    private val coupons: List<Coupon>,
    private val onBuyClicked: (Coupon) -> Unit
) : RecyclerView.Adapter<CouponAdapter.CouponViewHolder>() {

    inner class CouponViewHolder(val binding: ItemRewardsCouponBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CouponViewHolder {
        val binding = ItemRewardsCouponBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CouponViewHolder(binding)
    }

    override fun getItemCount(): Int = coupons.size

    override fun onBindViewHolder(holder: CouponViewHolder, position: Int) {
        val coupon = coupons[position]
        with(holder.binding) {
            tvBrandName.text = coupon.brandName
            tvCouponDescription.text = coupon.description
            tvCouponPrice.text = "💰 ${coupon.price} Coins"

            btnBuyCoupon.setOnClickListener {
                onBuyClicked(coupon)
            }
        }
    }
}