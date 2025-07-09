package com.ik.test.kt.bist.view.test

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ik.test.kt.bist.R
import com.ik.test.kt.bist.viewmodel.WifiTestViewModel

class TempTestFragment : Fragment() {

    companion object {
        fun newInstance() = WifiTestFragment()
    }

    private val viewModel: WifiTestViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Use the ViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_wifi_test, container, false)
    }
}