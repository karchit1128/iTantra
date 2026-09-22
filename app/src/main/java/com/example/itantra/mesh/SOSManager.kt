package com.example.itantra.mesh

import android.content.Context
import com.example.itantra.data.TriageEntity
import com.example.itantra.data.TriageRepository
import com.example.itantra.hardware.LocationEngine
import com.example.itantra.hardware.FlashlightManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

object SOSManager {
    fun sendSOS(context: Context, customMessage: String = "EMERGENCY: SOS") {
        val meshManager = MeshNetworkManager.getInstance(context)
        val triageRepo = TriageRepository.getInstance(context)
        
        val msgId = UUID.randomUUID().toString()
        val transcript = "SOS: $customMessage"
        
        // Bug 17 Fix: Use GlobalScope so we don't leak anonymous scopes on rapid SOS presses
        GlobalScope.launch(Dispatchers.IO) {
            val loc = LocationEngine.fetchLocation(context)
            val entity = TriageEntity(id = msgId, message = transcript, priority = "RED", latitude = loc.first, longitude = loc.second, isSentByMe = true)
            triageRepo.insertVictim(entity)
            meshManager.broadcastMessage("[ID:$msgId][TTS]$transcript")
            FlashlightManager.strobeSos(context)
        }
    }
}
