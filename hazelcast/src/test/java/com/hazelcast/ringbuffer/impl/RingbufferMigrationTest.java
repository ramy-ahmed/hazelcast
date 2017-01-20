package com.hazelcast.ringbuffer.impl;

import com.hazelcast.config.Config;
import com.hazelcast.config.RingbufferConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.test.HazelcastParallelClassRunner;
import com.hazelcast.test.HazelcastTestSupport;
import com.hazelcast.test.TestHazelcastInstanceFactory;
import com.hazelcast.test.annotation.ParallelTest;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(HazelcastParallelClassRunner.class)
@Category({QuickTest.class, ParallelTest.class})
public class RingbufferMigrationTest extends HazelcastTestSupport {

    public static final int CAPACITY = 100;
    private TestHazelcastInstanceFactory instanceFactory;

    @Before
    public void setup() {
        instanceFactory = createHazelcastInstanceFactory(3);
    }

    @Test
    public void test() throws Exception {
        final String ringbufferName = "ringbuffer";
        final Config config = new Config()
                .addRingBufferConfig(new RingbufferConfig(ringbufferName).setTimeToLiveSeconds(0));
        HazelcastInstance hz1 = instanceFactory.newHazelcastInstance(config);

        for (int k = 0; k < 10 * CAPACITY; k++) {
            hz1.getRingbuffer(ringbufferName).add(k);
        }

        long oldTailSeq = hz1.getRingbuffer(ringbufferName).tailSequence();
        long oldHeadSeq = hz1.getRingbuffer(ringbufferName).headSequence();

        HazelcastInstance hz2 = instanceFactory.newHazelcastInstance(config);
        HazelcastInstance hz3 = instanceFactory.newHazelcastInstance(config);

        assertClusterSizeEventually(3, hz2);
        hz1.shutdown();
        assertClusterSizeEventually(2, hz2);

        assertEquals(oldTailSeq, hz2.getRingbuffer(ringbufferName).tailSequence());
        assertEquals(oldHeadSeq, hz2.getRingbuffer(ringbufferName).headSequence());
    }
}
