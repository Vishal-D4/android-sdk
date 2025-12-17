package com.yourgpt.sdk.example

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.yourgpt.sdk.YourGPTConfig
import com.yourgpt.sdk.YourGPTSDK

class SupportFragment : Fragment() {
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_support, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Set up click listener for the chat button
        view.findViewById<View>(R.id.btnOpenChat).setOnClickListener {
            openChatSupport()
        }
    }
    
    private fun openChatSupport() {
        val configuration = YourGPTConfig(
            widgetUid = "69dd8b5d-d4bf-444c-a40f-732d15248ae9",
        )
        
        // Open the chatbot bottom sheet
        YourGPTSDK.openChatbotBottomSheet(parentFragmentManager, configuration)
    }
}