package com.example.drivelog.ui.components

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.drivelog.R
import com.example.drivelog.data.model.Trip
import com.example.drivelog.databinding.ItemTripBinding
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class TripAdapter : ListAdapter<Trip, TripAdapter.TripViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val binding = ItemTripBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TripViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TripViewHolder(
        private val binding: ItemTripBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(trip: Trip) {
            val context = binding.root.context
            binding.tripTime.text = context.getString(
                R.string.trip_time_range,
                formatTime(trip.start),
                formatTime(trip.end),
            )
            binding.tripPayment.text = paymentLabel(trip.payment)
            binding.tripAmount.text = context.getString(R.string.money_value, formatMoney(trip.amount))
            binding.tripCommission.text = context.getString(
                R.string.trip_commission,
                formatMoney(trip.commission),
            )
        }

        private fun paymentLabel(payment: String): String {
            return when (payment) {
                "cash" -> binding.root.context.getString(R.string.payment_cash)
                "card" -> binding.root.context.getString(R.string.payment_card)
                else -> payment
            }
        }

        private fun formatTime(isoDateTime: String): String {
            val parsers = listOf(
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                },
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US),
            )
            val output = SimpleDateFormat("HH:mm", Locale.getDefault())
            for (parser in parsers) {
                val parsed = runCatching { parser.parse(isoDateTime) }.getOrNull()
                if (parsed != null) {
                    return output.format(parsed)
                }
            }
            return isoDateTime
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Trip>() {
            override fun areItemsTheSame(oldItem: Trip, newItem: Trip): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Trip, newItem: Trip): Boolean = oldItem == newItem
        }
    }
}

fun formatMoney(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(Locale.getDefault(), "%.2f", value)
    }
}
