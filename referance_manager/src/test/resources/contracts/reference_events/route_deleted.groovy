import org.springframework.cloud.contract.spec.Contract

/**
 * reference.events :: ROUTE DELETED
 *
 * DELETED event'lerinde payload null gönderilir.
 */
Contract.make {
    label("triggerRouteDeleted")
    input {
        triggeredBy("triggerRouteDeleted()")
    }
    outputMessage {
        sentTo("reference.events")
        headers {
            header("kafka_messageKey", $(
                producer(regex("ROUTE:[A-Z]{4}-[A-Z]{4}")),
                consumer("ROUTE:LTFM-LTAC")
            ))
        }
        body([
            entityType  : $(producer(regex("AIRLINE|AIRCRAFT|STATION|ROUTE")), consumer("ROUTE")),
            changeType  : $(producer(regex("CREATED|UPDATED|DELETED")),        consumer("DELETED")),
            businessKey : $(producer(regex("^[A-Z]{4}-[A-Z]{4}\$")),           consumer("LTFM-LTAC")),
            payload     : null
        ])
    }
}
