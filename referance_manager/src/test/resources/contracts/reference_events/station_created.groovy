import org.springframework.cloud.contract.spec.Contract

/**
 * reference.events :: STATION CREATED
 *
 * businessKey = ICAO kodu (4 büyük harf, örn. "LTFM")
 * payload = StationResponse{id, icaoCode, name}
 *
 * ICAO formatı kritik — FAS ve FlightService bu kodu doğrudan key olarak kullanır.
 */
Contract.make {
    label("triggerStationCreated")
    input {
        triggeredBy("triggerStationCreated()")
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
            changeType  : $(producer(regex("CREATED|UPDATED|DELETED")),        consumer("CREATED")),
            businessKey : $(producer(regex("^[A-Z]{4}\$")),                    consumer("LTFM")),
            payload: [
                id       : $(producer(regex("[0-9]+")),       consumer(1)),
                icaoCode : $(producer(regex("^[A-Z]{4}\$")), consumer("LTFM")),
                name     : $(producer(regex(".+")),            consumer("Istanbul Airport"))
            ]
        ])
    }
}
