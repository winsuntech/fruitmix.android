package com.winsun.fruitmix.newdesign201804.equipment.add

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import com.winsun.fruitmix.BaseToolbarActivity
import com.winsun.fruitmix.R
import com.winsun.fruitmix.util.Util
import kotlinx.android.synthetic.main.activity_add_equipment_by_ip.*

class AddEquipmentByIpActivity : BaseToolbarActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        toolbarViewModel.navigationIconResId.set(R.drawable.red_clear)

        toolbarViewModel.showSelect.set(true)

        toolbarViewModel.setToolbarSelectBtnOnClickListener {

            handleEnterIpAddressFinished()

        }

    }

    private fun handleEnterIpAddressFinished() {

        val ipEditOneNum = getIpEditNum(ip_edit_one)
        val ipEditTwoNum = getIpEditNum(ip_edit_two)
        val ipEditThreeNum = getIpEditNum(ip_edit_three)
        val ipEditFourNum = getIpEditNum(ip_edit_four)

        if (ipEditOneNum == null || ipEditTwoNum == null || ipEditThreeNum == null || ipEditFourNum == null)
            return

        val ip = "$ipEditOneNum.$ipEditTwoNum.$ipEditThreeNum.$ipEditFourNum"

        val portEdit = port_edit.text.toString()

        val portNum: Int = (if (!portEdit.isEmpty()) {
            getIpEditNum(port_edit)
        } else
            3001) ?: return

        //TODO:return ip and port for search equipment

    }

    private fun getIpEditNum(editText: EditText): Int? {

        val ipEditOne = editText.text.toString()

        val ipEditOneNum = ipEditOne.toIntOrNull()

        if (ipEditOneNum == null || ipEditOneNum < 0 || ipEditOneNum > 255) {
            erro_ip_hint.visibility = View.VISIBLE
        } else
            erro_ip_hint.visibility = View.INVISIBLE

        return ipEditOneNum
    }


    override fun generateContent(root: ViewGroup?): View {

        return View.inflate(this, R.layout.activity_add_equipment_by_ip, root)

    }


    override fun getToolbarTitle(): String {
        return getString(R.string.add_equipment_by_ip)
    }

}
