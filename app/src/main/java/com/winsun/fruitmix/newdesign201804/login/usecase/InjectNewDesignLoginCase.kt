package com.winsun.fruitmix.newdesign201804.login.usecase

import android.content.Context
import com.winsun.fruitmix.http.InjectHttp
import com.winsun.fruitmix.newdesign201804.user.preference.InjectUserPreference
import com.winsun.fruitmix.stations.InjectStation
import com.winsun.fruitmix.system.setting.InjectSystemSettingDataSource
import com.winsun.fruitmix.token.data.InjectTokenRemoteDataSource
import com.winsun.fruitmix.user.datasource.InjectUser

class InjectNewDesignLoginCase {

    companion object {

        fun provideInstance(context: Context): NewDesignLoginUseCase {

            return NewDesignLoginUseCase(InjectTokenRemoteDataSource.provideTokenDataSource(context),
                    InjectSystemSettingDataSource.provideSystemSettingDataSource(context), InjectHttp.provideHttpRequestFactory(context),
                    InjectUser.provideRepository(context), InjectStation.provideStationDataSource(context),
                    InjectUserPreference.inject(context))

        }

    }

}