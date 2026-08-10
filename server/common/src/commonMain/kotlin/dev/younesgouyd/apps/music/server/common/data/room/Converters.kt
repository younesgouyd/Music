package dev.younesgouyd.apps.music.server.common.data.room

import androidx.room.TypeConverter
import dev.younesgouyd.apps.music.common.json
import dev.younesgouyd.apps.music.common.models.Inspection

object Converters {
    @TypeConverter
    fun fromContainerInspectionToJsonString(inspection: Inspection.Container?): String? {
        return inspection?.let { json.encodeToString<Inspection.Container>(it) }
    }

    @TypeConverter
    fun fromJsonStringToContainerInspection(jsonString: String?): Inspection.Container? {
        return jsonString?.let { json.decodeFromString<Inspection.Container>(it) }
    }

    @TypeConverter
    fun fromInspectionItemToJsonString(inspectionItemInspection: Inspection.Item): String {
        return json.encodeToString<Inspection.Item>(inspectionItemInspection)
    }

    @TypeConverter
    fun fromJsonStringToInspectionItem(jsonString: String): Inspection.Item {
        return json.decodeFromString<Inspection.Item>(jsonString)
    }
}