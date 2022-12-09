/*
 * Copyright (C) 2019 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.devbyteviewer.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.android.devbyteviewer.R
import com.example.android.devbyteviewer.databinding.DevbyteItemBinding
import com.example.android.devbyteviewer.databinding.FragmentDevByteBinding
import com.example.android.devbyteviewer.domain.DevByteVideo
import com.example.android.devbyteviewer.viewmodels.DevByteViewModel

/**
 * menampilkan daftar dari DevBytes ke layar.
 */
class DevByteFragment : Fragment() {

    /**
     * menggunakan lazy untuk menunda pembuatan viewModel sampai method lifecycle tepat.
     * memerlukan viewModel yang tidak di refrensi sebelum onActivityCreated.
     */
    private val viewModel: DevByteViewModel by lazy {
        val activity = requireNotNull(this.activity) {
            "You can only access the viewModel after onActivityCreated()"
        }
        ViewModelProvider(this, DevByteViewModel.Factory(activity.application))
                .get(DevByteViewModel::class.java)
    }

    /**
     * RecyclerView Adapter untuk mengkonfersi daftar dari Video ke cards.
     */
    private var viewModelAdapter: DevByteAdapter? = null

    /**
     * langsung di panggil setelah onCreateView() telah dikembalikan, and fragment
     * view hierarchy telah dibuat. ini bisa digunakan untuk initialization akhir
     * seperti mengembalikan view atau mengembalikan state.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.playlist.observe(viewLifecycleOwner, Observer<List<DevByteVideo>> { videos ->
            videos?.apply {
                viewModelAdapter?.videos = videos
            }
        })
    }

    /**
     * dipanggil untuk mendapatkan fragment instantiate dari user interface view.
     *
     * @param inflater objek LayoutInflater yang dapat digunakan untuk mengembang
     * setiap tampilan dalam fragmen
     * @param container jika bukan null, merupakan tampilan induk dari fragmen
     * UI harus dilampirkan. Fragmen tidak boleh menambahkan tampilan itu sendiri,
     * tapi ini bisa digunakan untuk menghasilkan LayoutParams dari tampilan.
     * @param savedInstanceState jika bukan null, fragmen ini sedang dibuat ulang
     * dari status tersimpan sebelumnya seperti yang diberikan di sini
     *
     * @return Mengembalikan Tampilan untuk UI fragmen.
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val binding: FragmentDevByteBinding = DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_dev_byte,
                container,
                false)
        // mengeset lifecycleOwner sehingga DataBinding bisa mengobservasi LiveData
        binding.setLifecycleOwner(viewLifecycleOwner)

        binding.viewModel = viewModel

        viewModelAdapter = DevByteAdapter(VideoClick {
            // ketika video di klik blok atau lambda akan dipanggil oleh DevByteAdapter
            val packageManager = context?.packageManager ?: return@VideoClick

            // mencoba membangun direct intent untuk YouTube app
            var intent = Intent(Intent.ACTION_VIEW, it.launchUri)
            if(intent.resolveActivity(packageManager) == null) {
                // jika YouTube tidak ditemukan, akan menggunkana web ur
                intent = Intent(Intent.ACTION_VIEW, Uri.parse(it.url))
            }

            startActivity(intent)
        })

        binding.root.findViewById<RecyclerView>(R.id.recycler_view).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = viewModelAdapter
        }


        // Observer untuk jaringan error.
        viewModel.eventNetworkError.observe(viewLifecycleOwner, Observer<Boolean> { isNetworkError ->
            if (isNetworkError) onNetworkError()
        })

        return binding.root
    }

    /**
     * Method untuk menampilkan pesan Toast error untuk jaringan errors.
     */
    private fun onNetworkError() {
        if(!viewModel.isNetworkErrorShown.value!!) {
            Toast.makeText(activity, "Network Error", Toast.LENGTH_LONG).show()
            viewModel.onNetworkErrorShown()
        }
    }

    /**
     * Helper method ntuk membangun link aplikasi YouTube
     */
    private val DevByteVideo.launchUri: Uri
        get() {
            val httpUri = Uri.parse(url)
            return Uri.parse("vnd.youtube:" + httpUri.getQueryParameter("v"))
        }
}

/**
 * Click listener untuk Videos. dengan memberikan nama block membantu pengguna memahami apa yang terjadi.
 */
class VideoClick(val block: (DevByteVideo) -> Unit) {
    /**
     * dipangil ketika video di klik
     */
    fun onClick(video: DevByteVideo) = block(video)
}

/**
 * RecyclerView Adapter  untuk mengatur data binding pada items dalam daftar.
 */
class DevByteAdapter(val callback: VideoClick) : RecyclerView.Adapter<DevByteViewHolder>() {

    /**
     * akan menampilkan videos dari Adapter
     */
    var videos: List<DevByteVideo> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    /**
     * akan dipanggil pada saat RecyclerView perlu {@link ViewHolder} baru pada type untuk mewakili
     * item
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DevByteViewHolder {
        val withDataBinding: DevbyteItemBinding = DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                DevByteViewHolder.LAYOUT,
                parent,
                false)
        return DevByteViewHolder(withDataBinding)
    }

    override fun getItemCount() = videos.size

    /**
     * akan dipanggil oleh RecyclerView untuk menampilkan data pada posisi spesifik. method ini harus
     * memperbarui conten {@link ViewHolder#itemView} untuk merefleksi item pada posisi ditentukan.
     */
    override fun onBindViewHolder(holder: DevByteViewHolder, position: Int) {
        holder.viewDataBinding.also {
            it.video = videos[position]
            it.videoCallback = callback
        }
    }

}

/**
 * ViewHolder untuk DevByte items. pekerjaan diselesaikan dengan data binding.
 */
class DevByteViewHolder(val viewDataBinding: DevbyteItemBinding) :
        RecyclerView.ViewHolder(viewDataBinding.root) {
    companion object {
        @LayoutRes
        val LAYOUT = R.layout.devbyte_item
    }
}