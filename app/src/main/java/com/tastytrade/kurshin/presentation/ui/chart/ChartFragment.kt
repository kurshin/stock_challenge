package com.tastytrade.kurshin.presentation.ui.chart

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.github.mikephil.charting.charts.CandleStickChart
import com.tastytrade.kurshin.R
import com.tastytrade.kurshin.databinding.FragmentChartBinding
import com.tastytrade.kurshin.domain.Symbol

class ChartFragment : Fragment() {

    private val viewModel: ChartViewModel by viewModels { ViewModelFactory }
    private lateinit var symbol: Symbol

    private val candleStickChart: CandleStickChart by lazy {
        requireActivity().findViewById(R.id.candleStickChart)
    }

    private var _binding: FragmentChartBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            symbol = it.getSerializable(ARG_SYMBOL) as Symbol
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.error.observe(viewLifecycleOwner) {
            Toast.makeText(requireActivity(), it, Toast.LENGTH_SHORT).show()
        }

        viewModel.chart.observe(viewLifecycleOwner) {
            CandleHelper.setUpCandleChart(candleStickChart, it)
        }

        viewModel.quote.observe(viewLifecycleOwner) {
            binding.tvLastPrice.text = getString(R.string.last_, String.format("%.2f", it.lastPrice))
            binding.tvAskPrice.text = getString(R.string.ask_, String.format("%.2f", it.askPrice))
            binding.tvBidPrice.text = getString(R.string.bid_, String.format("%.2f", it.bidPrice))
        }

        setUpChart()
        viewModel.getChartData(symbol.name)
        viewModel.getQuoteDataRepeatedly(symbol.name)
    }

    private fun setUpChart() {
        candleStickChart.setNoDataText(getString(R.string.loading))
        candleStickChart.invalidate()
        binding.tvSymbolName.text = symbol.name
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {

        private const val ARG_SYMBOL = "arg_symbol"
        fun newInstance(symbol: Symbol): ChartFragment {
            val fragment = ChartFragment()
            val args = Bundle()
            args.putSerializable(ARG_SYMBOL, symbol)
            fragment.arguments = args
            return fragment
        }
    }
}