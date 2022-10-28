/*
 * Copyright 2019 Punch Through Design LLC
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
package de.tuberlin.mcc.simra.app.util.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import de.tuberlin.mcc.simra.app.util.ConnectionManager

/** A listener containing callback methods to be registered with [ConnectionManager].*/
open class ConnectionEventListener {
    open var onConnectionSetupComplete: ((BluetoothGatt) -> Unit)? = null
    open var onScanStart:((Boolean) -> Unit)? = null
    open var onScanStop:((Boolean) -> Unit)? = null
    open var onDisconnect: ((BluetoothDevice) -> Unit)? = null
    open var onDescriptorRead: ((BluetoothDevice, BluetoothGattDescriptor) -> Unit)? = null
    open var onDescriptorWrite: ((BluetoothDevice, BluetoothGattDescriptor) -> Unit)? = null
    open var onCharacteristicChanged: ((BluetoothDevice, BluetoothGattCharacteristic) -> Unit)? = null
    open var onCharacteristicRead: ((BluetoothDevice, BluetoothGattCharacteristic) -> Unit)? = null
    open var onNotificationsEnabled: ((BluetoothDevice, BluetoothGattCharacteristic) -> Unit)? = null
    open var onNotificationsDisabled: ((BluetoothDevice, BluetoothGattCharacteristic) -> Unit)? = null
    open var onDeviceFound:((BluetoothDevice) -> Unit)? = null
    open var onSensorDistanceNotification:((ConnectionManager.Measurement) -> Unit)? = null
    open var onClosePassNotification:((ConnectionManager.Measurement) -> Unit)? = null
    open var onTimeRead:((Int) -> Unit)? = null
    open var onConnectionFailed: ((BluetoothDevice) -> Unit)? = null
}