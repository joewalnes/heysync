package org.fotap.heysync;

import org.jetlang.fibers.ThreadFiber;

/**
 * @author <a href="mailto:peter.royal@pobox.com">peter royal</a>
 */
public class HeysyncExample {

    public static void main(String... args) throws InterruptedException {
        // Create our fiber
        ThreadFiber fiber = new ThreadFiber();
        fiber.start();

        // Create protocol
        Protocol<Mouse> protocol = Protocol.create(Mouse.class);

        // Create our FieldMouse and subscribe him to the protocol
        FieldMouse fieldMouse = new FieldMouse();
        protocol.subscribe(fiber, fieldMouse);

        // Get the publisher for all registered Mice
        Mouse publisher = protocol.publisher();

        // Tell our mouse to eat cheese and provoke cats
        publisher.eatCheese("cheddar");
        publisher.provokeCats(4);

        fieldMouse.latch.await();
    }
}