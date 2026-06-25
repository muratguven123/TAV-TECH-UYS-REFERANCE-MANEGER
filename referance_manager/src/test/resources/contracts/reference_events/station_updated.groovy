import org.springframework.cloud.contract.spec.Contract

/**
 * reference.events :: STATION UPDATED
 */
Contract.make {
    label("triggerStationUpdated")
    input {
        triggeredBy("triggerStationUpdated()")
    }
    outputMessage {
        sentTo("reference.events")
        headers {
            header("kafka_messageKey", $(
                producer(regex("STATION:[A-Z]{4}")),
                consumer("STATION:LTFM")
            ))
        }
        body([
            entityType  : $(producer(regex("AIRLINE|AIRCRAFT|STATION|ROUTE")), consumer("STATION")),
            changeType  : $(producer(regex("CREATED|UPDATED|DELETED")),        consumer("UPDATED")),
            businessKey : $(producer(regex("^[A-Z]{4}\$")),                    consumer("LTFM")),
            payload: [
                id       : $(producer(regex("[0-9]+")),       consumer(1)),
                icaoCode : $(producer(regex("^[A-Z]{4}\$")), consumer("LTFM")),
                name     : $(producer(regex(".+")),            consumer("Istanbul Airport Revised"))
            ]
        ])
    }
}
