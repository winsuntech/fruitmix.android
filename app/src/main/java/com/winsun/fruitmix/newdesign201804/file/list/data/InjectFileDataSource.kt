package com.winsun.fruitmix.newdesign201804.file.list.data

import android.content.Context

class InjectFileDataSource{

    companion object {
        fun inject(context: Context): FileDataSource {
            return FakeFileDataSource()
        }
    }

}