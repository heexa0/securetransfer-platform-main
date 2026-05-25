<<<<<<< HEAD
package com.securetransfer.platform.transaction.fraud;

public sealed interface FraudResult {
    record Clean() implements FraudResult {
    }

    record Suspicious(String reason, double score) implements FraudResult {
    }

    record Blocked(String reason) implements FraudResult {
    }
}
=======
package com.securetransfer.platform.transaction.fraud;

public sealed interface FraudResult {
    record Clean() implements FraudResult {
    }

    record Suspicious(String reason, double score) implements FraudResult {
    }

    record Blocked(String reason) implements FraudResult {
    }
}
>>>>>>> 3ba8522ccea825626175d2122bcfce25d088fc90
