package com.st10321779.rootedwealth.ui.rewards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.st10321779.rootedwealth.databinding.FragmentCouponsBinding
import com.st10321779.rootedwealth.util.PrefsManager

class CouponsFragment : Fragment() {

    private var _binding: FragmentCouponsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCouponsBinding.inflate(inflater, container, false)

        val coupons = createMockCoupons()

        val adapter = CouponAdapter(coupons) { coupon ->
            // On Buy Clicked
            if (PrefsManager.spendCoins(requireContext(), coupon.price)) {
                (activity as? RewardsActivity)?.updateCoinBalance()
                showQrCodeDialog(coupon)
            } else {
                Toast.makeText(context, "You need ${coupon.price} coins!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.rvCoupons.layoutManager = LinearLayoutManager(context)
        binding.rvCoupons.adapter = adapter

        return binding.root
    }

    private fun showQrCodeDialog(coupon: Coupon) {
        AlertDialog.Builder(requireContext())
            .setTitle("${coupon.brandName} Coupon")
            .setMessage("Coupon purchased! \n\n'${coupon.description}'\n\n(${coupon.qrCodeContent})")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun createMockCoupons(): List<Coupon> {
        return listOf(
            Coupon("nandos_10", "Nando's", "10% off your next meal", 200, "NDS-123-XYZ"),
            Coupon("kfc_5", "KFC", "R5 off any Streetwise meal", 100, "KFC-456-ABC"),
            Coupon("checkers_20", "Checkers", "R20 off when you spend R200", 350, "CHK-789-DEF"),
            Coupon("woolies_coffee", "Woolworths Cafe", "Free coffee with any cake", 150, "WWH-321-GHI")
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class Coupon(
    val id: String,
    val brandName: String,
    val description: String,
    val price: Int,
    val qrCodeContent: String
)