/* @java.file.header */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.kernal.processors.cache.distributed.dht;

import org.gridgain.grid.cache.*;
import org.gridgain.grid.kernal.processors.cache.distributed.dht.preloader.*;

import static org.gridgain.grid.cache.GridCacheMemoryMode.*;

/**
 * Test cases for partitioned cache {@link GridDhtPreloader preloader} with off-heap value storage.
 */
public class GridCacheDhtPreloadOffHeapSelfTest extends GridCacheDhtPreloadSelfTest {
    /** {@inheritDoc} */
    @Override protected GridCacheConfiguration cacheConfiguration(String gridName) {
        GridCacheConfiguration cacheCfg = super.cacheConfiguration(gridName);

        cacheCfg.setQueryIndexEnabled(false);
        cacheCfg.setMemoryMode(OFFHEAP_VALUES);
        cacheCfg.setOffHeapMaxMemory(0);

        return cacheCfg;
    }
}