package com.seoulhankuko.app.domain.model

import com.seoulhankuko.app.data.database.entities.Course
import com.seoulhankuko.app.data.database.entities.UnitEntity

data class CourseWithUnits(
    val course: Course,
    val units: List<UnitEntity>
)
