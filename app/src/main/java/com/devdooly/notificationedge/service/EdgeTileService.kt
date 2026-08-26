package com.devdooly.notificationedge.service

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi

/**
 * 상단바 빠른 설정(Quick Settings) 타일 서비스:
 * 클릭 시 액티비티(Activity) 없이 백그라운드 서비스로 바로 오버레이 패널을 토글하여 유튜브 PiP를 차단함
 */
@RequiresApi(Build.VERSION_CODES.N)
class EdgeTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.let {
            it.state = Tile.STATE_ACTIVE
            it.label = "알림 엣지"
            it.updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, EdgeOverlayService::class.java).apply {
            action = EdgeOverlayService.ACTION_TOGGLE_PANEL
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}
