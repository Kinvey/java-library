package com.kinvey.java.model

import junit.framework.TestCase

class KinveySaveBatchResponseTest : TestCase() {

    var response: KinveySaveBatchResponse<Entity>? = null

    fun testConstructor() {

        response = KinveySaveBatchResponse()

        assertEquals(null, response?.entities)
        assertEquals(null, response?.errors)
        assertTrue(response?.haveErrors == false)

        val entities = listOf(Entity("test1"), Entity("test2"))
        val errors = listOf(KinveyBatchInsertError(0, 400, "test error"))

        response?.entities = entities
        response?.errors = errors

        assertEquals(entities, response?.entities)
        assertEquals(errors, response?.errors)
        assertTrue(response?.haveErrors == true)
    }

    data class Entity (
        var name: String = ""
    )
}