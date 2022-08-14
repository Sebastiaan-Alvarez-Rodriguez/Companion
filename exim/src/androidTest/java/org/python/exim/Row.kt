package org.python.exim

class Row(var id: Long, var name: String, var age: Int) : Exportable, Importable<Row> {

    constructor() : this(0, "", 42)

    override fun values(): Array<EximUtil.FieldInfo> {
        return arrayOf(
            EximUtil.FieldInfo(id, "id"),
            EximUtil.FieldInfo(name, "name"),
            EximUtil.FieldInfo(age, "age")
        )
    }

    override val amountValues: Int = values().size

    override fun fromValues(values: List<Any?>): Row {
        return Row(
            values[0] as Long,  // id
            values[1] as String,  // name
            values[2] as Int // age
        )
    }

    override fun hashCode(): Int {
        return java.lang.Long.hashCode(id) // id's should be unique and thus be perfect for hashing.
    }

    override fun equals(obj: Any?): Boolean {
        // For testing purposes, we do a full comparison between Rows
        // to show all fields match after reading/writing
        return obj is Row && id == obj.id &&
                name == obj.name && age == obj.age
    }
}