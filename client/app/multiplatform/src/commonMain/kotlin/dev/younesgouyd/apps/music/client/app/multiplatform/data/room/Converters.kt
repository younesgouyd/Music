package dev.younesgouyd.apps.music.client.app.multiplatform.data.room

import androidx.room.TypeConverter
import dev.younesgouyd.apps.music.common.Inspection
import dev.younesgouyd.apps.music.common.json

object Converters {
    @TypeConverter
    fun fromContainerInspectionToJsonString(inspection: Inspection.ContainerInspection?): String? {
        return inspection?.let { json.encodeToString<Inspection.ContainerInspection>(it) }
    }

    @TypeConverter
    fun fromJsonStringToContainerInspection(jsonString: String?): Inspection.ContainerInspection? {
        return jsonString?.let { json.decodeFromString<Inspection.ContainerInspection>(it) }
    }

    @TypeConverter
    fun fromInspectionItemToJsonString(inspectionItemInspection: Inspection.ItemInspection): String {
        return json.encodeToString<Inspection.ItemInspection>(inspectionItemInspection)
    }

    @TypeConverter
    fun fromJsonStringToInspectionItem(jsonString: String): Inspection.ItemInspection {
        return json.decodeFromString<Inspection.ItemInspection>(jsonString)
    }
}