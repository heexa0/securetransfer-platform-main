<<<<<<< HEAD
package com.securetransfer.platform.transaction.event;

import com.securetransfer.platform.transaction.entity.Transaction;
import org.springframework.context.ApplicationEvent;

public class TransactionCreatedEvent extends ApplicationEvent {
    private final Transaction transaction;

    public TransactionCreatedEvent(Object source, Transaction transaction) {
        super(source);
        this.transaction = transaction;
    }

    public Transaction getTransaction() {
        return transaction;
    }
}
=======
package com.securetransfer.platform.transaction.event;

import com.securetransfer.platform.transaction.entity.Transaction;
import org.springframework.context.ApplicationEvent;

public class TransactionCreatedEvent extends ApplicationEvent {
    private final Transaction transaction;

    public TransactionCreatedEvent(Object source, Transaction transaction) {
        super(source);
        this.transaction = transaction;
    }

    public Transaction getTransaction() {
        return transaction;
    }
}
>>>>>>> 3ba8522ccea825626175d2122bcfce25d088fc90
