package com.commandiron.wheel_picker_compose.core

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import java.time.LocalTime
import java.time.temporal.ChronoUnit

@RequiresApi(Build.VERSION_CODES.O)
@Composable
internal fun DefaultWheelTimePicker(
    modifier: Modifier = Modifier,
    startTime: LocalTime = LocalTime.now(),
    timeFormat: TimeFormat = TimeFormat.HOUR_24,
    backwardsDisabled: Boolean = false,
    size: DpSize = DpSize(128.dp, 128.dp),
    textStyle: TextStyle = MaterialTheme.typography.titleMedium,
    textColor: Color = LocalContentColor.current,
    selectorProperties: SelectorProperties = WheelPickerDefaults.selectorProperties(),
    onSnappedTime : (snappedTime: SnappedTime) -> Int? = { _ -> null },
) {

    var snappedTime by remember { mutableStateOf(startTime.truncatedTo(ChronoUnit.MINUTES)) }

    val hours = (0..23).map {
        Hour(
            text = it.toString().padStart(2, '0'),
            value = it,
            index = it
        )
    }
    val amPmHours = (1..12).map {
        AmPmHour(
            text = it.toString(),
            value = it,
            index = it - 1
        )
    }

    val minutes = (0..59).map {
        Minute(
            text = it.toString().padStart(2, '0'),
            value = it,
            index = it
        )
    }

    val amPms = listOf(
        AmPm(
            text = "AM",
            value = AmPmValue.AM,
            index = 0
        ),
        AmPm(
            text = "PM",
            value = AmPmValue.PM,
            index = 1
        )
    )

    var snappedAmPm by remember {
        mutableStateOf(
            amPms.find { it.value == amPmValueFromTime(startTime) } ?: amPms[0]
        )
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center){
        if(selectorProperties.enabled().value){
            Surface(
                modifier = Modifier.size(size.width,size.height / 3),
                shape = selectorProperties.shape().value,
                color = selectorProperties.color().value,
                border = selectorProperties.border().value
            ) {}
        }
        Row {
            //Hour
            WheelTextPicker(
                size = DpSize(
                    width = size.width / if(timeFormat == TimeFormat.HOUR_24) 2 else 3,
                    height = size.height
                ),
                texts = if(timeFormat == TimeFormat.HOUR_24) hours.map { it.text } else amPmHours.map { it.text },
                style = textStyle,
                color = textColor,
                startIndex =  if(timeFormat == TimeFormat.HOUR_24) {
                    hours.find { it.value == startTime.hour }?.index ?: 0
                } else amPmHours.find { it.value ==  hourToAmPmHour(startTime.hour) }?.index ?: 0,
                selectorProperties = WheelPickerDefaults.selectorProperties(
                    enabled = false
                ),
                onScrollFinished = { snappedIndex ->

                    val newHour = if(timeFormat == TimeFormat.HOUR_24) {
                        hours.find { it.index == snappedIndex }?.value
                    } else {
                        amPmHourToHour(
                            amPmHours.find { it.index == snappedIndex }?.value ?: 0,
                            snappedAmPm.value
                        )
                    }

                    newHour?.let {

                        val newTime = snappedTime.withHour(newHour)

                        val isTimeBefore = isTimeBefore(newTime, startTime)

                        if (backwardsDisabled) {
                            if (!isTimeBefore) {
                                snappedTime = newTime
                            }
                        } else {
                            snappedTime = newTime
                        }

                        val newIndex = if(timeFormat == TimeFormat.HOUR_24) {
                            hours.find { it.value == snappedTime.hour }?.index
                        } else {
                            amPmHours.find { it.value ==  hourToAmPmHour(snappedTime.hour)}?.index
                        }

                        newIndex?.let {
                            onSnappedTime(
                                SnappedTime.Hour(
                                    localTime = snappedTime,
                                    index = newIndex
                                )
                            )?.let { return@WheelTextPicker it }
                        }
                    }

                    return@WheelTextPicker if(timeFormat == TimeFormat.HOUR_24) {
                        hours.find { it.value == snappedTime.hour }?.index
                    } else {
                        amPmHours.find { it.value ==  hourToAmPmHour(snappedTime.hour)}?.index
                    }
                }
            )
            //Minute
            WheelTextPicker(
                size = DpSize(
                    width = size.width / if(timeFormat == TimeFormat.HOUR_24) 2 else 3,
                    height = size.height
                ),
                texts = minutes.map { it.text },
                style = textStyle,
                color = textColor,
                startIndex = minutes.find { it.value == startTime.minute }?.index ?: 0,
                selectorProperties = WheelPickerDefaults.selectorProperties(
                    enabled = false
                ),
                onScrollFinished = { snappedIndex ->

                    val newMinute = minutes.find { it.index == snappedIndex }?.value

                    val newHour = if(timeFormat == TimeFormat.HOUR_24) {
                        hours.find { it.value == snappedTime.hour }?.value
                    } else {
                        amPmHourToHour(
                            amPmHours.find { it.value == hourToAmPmHour(snappedTime.hour) }?.value ?: 0,
                            snappedAmPm.value
                        )
                    }

                    newMinute?.let {
                        newHour?.let {
                            val newTime = snappedTime.withMinute(newMinute).withHour(newHour)

                            val isTimeBefore = isTimeBefore(newTime, startTime)

                            if(backwardsDisabled){
                                if(!isTimeBefore){
                                    snappedTime = newTime
                                }
                            }else{
                                snappedTime = newTime
                            }

                            val newIndex = minutes.find { it.value == snappedTime.minute }?.index

                            newIndex?.let {
                                onSnappedTime(
                                    SnappedTime.Minute(
                                        localTime = snappedTime,
                                        index = newIndex
                                    )
                                )?.let { return@WheelTextPicker it }
                            }
                        }
                    }

                    return@WheelTextPicker minutes.find { it.value == snappedTime.minute }?.index
                }
            )
            //AM_PM
            if(timeFormat == TimeFormat.AM_PM) {
                WheelTextPicker(
                    size = DpSize(
                        width = size.width / 3,
                        height = size.height
                    ),
                    texts = amPms.map { it.text },
                    style = textStyle,
                    color = textColor,
                    startIndex = amPms.find { it.value == amPmValueFromTime(startTime) }?.index ?:0,
                    selectorProperties = WheelPickerDefaults.selectorProperties(
                        enabled = false
                    ),
                    onScrollFinished = { snappedIndex ->

                        val newAmPm =  amPms.find { it.index == snappedIndex }

                        newAmPm?.let {
                            snappedAmPm = newAmPm
                        }

                        val newMinute = minutes.find { it.value == snappedTime.minute }?.value

                        val newHour = amPmHourToHour(
                            amPmHours.find { it.value == hourToAmPmHour(snappedTime.hour) }?.value ?: 0,
                            snappedAmPm.value
                        )

                        newMinute?.let {
                            val newTime = snappedTime.withMinute(newMinute).withHour(newHour)

                            val isTimeBefore = isTimeBefore(newTime, startTime)

                            if(backwardsDisabled){
                                if(!isTimeBefore){
                                    snappedTime = newTime
                                }
                            }else{
                                snappedTime = newTime
                            }

                            val newIndex = minutes.find { it.value == snappedTime.hour }?.index

                            newIndex?.let {
                                onSnappedTime(
                                    SnappedTime.Hour(
                                        localTime = snappedTime,
                                        index = newIndex
                                    )
                                )
                            }
                        }

                        return@WheelTextPicker snappedIndex
                    }
                )
            }
        }
        Box(
            modifier = Modifier
                .size(
                    width = if (timeFormat == TimeFormat.HOUR_24) {
                        size.width
                    } else size.width * 2 / 3,
                    height = size.height / 3
                )
                .align(
                    alignment = if (timeFormat == TimeFormat.HOUR_24) {
                        Alignment.Center
                    } else Alignment.CenterStart
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = ":",
                style = textStyle,
                color = textColor
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun isTimeBefore(time: LocalTime, currentTime: LocalTime): Boolean{
    return time.isBefore(currentTime)
}

enum class TimeFormat {
    HOUR_24, AM_PM
}

private data class Hour(
    val text: String,
    val value: Int,
    val index: Int
)
private data class AmPmHour(
    val text: String,
    val value: Int,
    val index: Int
)

private fun hourToAmPmHour(hour: Int): Int {
    if(hour == 0) {
        return 12
    }
    if(hour <= 12) {
        return hour
    }
    return hour - 12
}

private fun amPmHourToHour(amPmHour: Int, amPmValue: AmPmValue): Int {
    return when(amPmValue) {
        AmPmValue.AM -> {
            amPmHour
        }
        AmPmValue.PM -> {
            if(amPmHour == 12) {
                0
            } else {
                amPmHour + 12
            }
        }
    }
}

private data class Minute(
    val text: String,
    val value: Int,
    val index: Int
)

private data class AmPm(
    val text: String,
    val value: AmPmValue,
    val index: Int?
)

internal enum class AmPmValue {
    AM, PM
}

@RequiresApi(Build.VERSION_CODES.O)
private fun amPmValueFromTime(time: LocalTime): AmPmValue {
    return if(time.hour > 11) AmPmValue.PM else AmPmValue.AM
}











