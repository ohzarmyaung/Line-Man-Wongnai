package com.linemanwongnai.app.view.home

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.linemanwongnai.app.R
import com.linemanwongnai.app.databinding.ActivityHomeBinding
import com.linemanwongnai.app.databinding.CoinDetailViewBottomSheetBinding
import com.linemanwongnai.app.model.Status
import com.linemanwongnai.app.utils.Utils
import dagger.hilt.android.AndroidEntryPoint
import java.lang.NumberFormatException
import java.text.DecimalFormat
import javax.inject.Inject


@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {

    lateinit var binding: ActivityHomeBinding

    @Inject
    lateinit var adapter: CoinListAdapter

    private val viewModel: HomeViewModel by viewModels()
    private var isRefreshing = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initObservable()
        initView()
    }

    private fun initView() {

        binding.recyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.adapter = adapter
        binding.refreshLayout.setOnRefreshListener {
            viewModel.getCoinList()
            isRefreshing = true
        }
    }

    private fun initObservable() {
        viewModel.liveData.observe(this) { result ->
            when (result.status) {
                Status.SUCCESS -> {
                    binding.loading.visibility = View.GONE
                    val coinList = result?.data
                    if (!coinList.isNullOrEmpty()) {
                        binding.textViewEmpty.visibility = View.GONE
                        binding.recyclerView.visibility = View.VISIBLE
                        binding.refreshLayout.isRefreshing = false

                        // get top rank three coin
                        val topRankThreeCoin =
                            coinList.filter { it.rank == 1 || it.rank == 2 || it.rank == 3 }

                        adapter.addData(coinList, isRefreshing)
                        adapter.addTopRankThreeCoin(topRankThreeCoin)

                    } else {
                        binding.textViewEmpty.visibility = View.VISIBLE
                        binding.recyclerView.visibility = View.GONE
                    }
                }

                Status.ERROR -> {
                    binding.loading.visibility = View.GONE
                    binding.refreshLayout.isRefreshing = false
                    val message =
                        if (!result?.error?.message.isNullOrEmpty()) result?.error?.message.toString() else getString(
                            R.string.label_unknown_error
                        )
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }

                Status.LOADING -> {
                    binding.loading.visibility = View.VISIBLE
                }
            }
        }

        adapter.itemClick.observe(this) { coinModel ->
            if (coinModel != null) {
                viewModel.getCoinDetail(coinModel.uuid)
            }
        }

        viewModel.coinDetailLiveData.observe(this) { result ->
            val coinModel = result?.data
            if (coinModel != null) {
                val dialog = BottomSheetDialog(this)
                val bottomSheetBinding =
                    CoinDetailViewBottomSheetBinding.inflate(layoutInflater, null, false)
                bottomSheetBinding.buttonGoToWebsite.setOnClickListener {
                    dialog.dismiss()
                    if (coinModel.websiteUrl != null) {
                        val browserIntent =
                            Intent(Intent.ACTION_VIEW, Uri.parse(coinModel.websiteUrl))
                        startActivity(browserIntent)
                    }
                }
                Utils.setImage(this, coinModel.iconUrl, bottomSheetBinding.imageCoinIcon)
                bottomSheetBinding.textViewCoinName.text = coinModel.name
                if (!coinModel.textColor.isNullOrEmpty()) {
                    if (coinModel.textColor!!.length == 6) {
                        bottomSheetBinding.textViewCoinName.setTextColor(Color.parseColor(coinModel.textColor))
                    } else {
                        bottomSheetBinding.textViewCoinName.setTextColor(
                            ContextCompat.getColor(
                                this,
                                R.color.color_textView_coin_name
                            )
                        )
                    }
                } else {
                    bottomSheetBinding.textViewCoinName.setTextColor(
                        ContextCompat.getColor(
                            this,
                            R.color.color_textView_coin_name
                        )
                    )
                }

                bottomSheetBinding.textViewCoinSymbol.text =
                    getString(R.string.label_coin_symbol, coinModel.symbol)

                val priceFormat = DecimalFormat("#,###.##")
                try {
                    bottomSheetBinding.textViewPrice.text = getString(
                        R.string.label_coin_price,
                        priceFormat.format(coinModel.price)
                    )
                } catch (_: Exception) {
                }

                // check million, billion, trillion
                if (coinModel.marketCap != null) {
                    try {
                        val marketCap = coinModel.marketCap!!.toLong()
                        if (marketCap >= Utils.MILLION && marketCap < Utils.BILLION) {
                            val million = marketCap / Utils.MILLION
                            bottomSheetBinding.textViewMarketCap.text =
                                getString(R.string.label_price_million, priceFormat.format(million))
                        } else if (marketCap >= Utils.BILLION && marketCap < Utils.TRILLION) {
                            val billion = marketCap / Utils.BILLION
                            bottomSheetBinding.textViewMarketCap.text =
                                getString(R.string.label_price_billion, priceFormat.format(billion))
                        } else if (marketCap >= Utils.TRILLION) {
                            val trillion = marketCap / Utils.TRILLION
                            bottomSheetBinding.textViewMarketCap.text =
                                getString(
                                    R.string.label_price_trillion,
                                    priceFormat.format(trillion)
                                )
                        } else {
                            bottomSheetBinding.textViewMarketCap.text =
                                getString(R.string.label_coin_price, priceFormat.format(marketCap))
                        }
                    } catch (_: Exception) {
                    }
                }
                bottomSheetBinding.textViewDescription.text = coinModel.description
                dialog.setCancelable(true)
                dialog.setContentView(bottomSheetBinding.root)
                dialog.show()
            }
        }
    }
}