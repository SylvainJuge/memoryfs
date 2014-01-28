package com.github.sylvainjuge.memoryfs;

import com.github.sylvainjuge.memoryfs.MemoryFileStore;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class MemoryFileStoreTest {

    @Test
    public void createWithDefaultSettings() throws IOException {
        MemoryFileStore store = MemoryFileStore.builder().build();
        assertThat(store.name()).isEqualTo("");
        assertThat(store.type()).isEqualTo("memory");
        assertThat(store.getTotalSpace()).isEqualTo(0);
        assertThat(store.getUnallocatedSpace()).isEqualTo(0);
        assertThat(store.getUsableSpace()).isEqualTo(0);
        assertThat(store.isReadOnly()).isFalse();
    }

    @Test
    public void createReadOnly() throws IOException {
        MemoryFileStore store = MemoryFileStore.builder()
                .readOnly(true)
                .capacity(100)
                .build();
        assertThat(store.isReadOnly()).isTrue();

        assertThat(store.getTotalSpace()).isEqualTo(100);
        assertThat(store.getUnallocatedSpace()).isEqualTo(100);
        // no space is usable as long as store is read-only
        assertThat(store.getUsableSpace()).isEqualTo(0);
    }

    @Test
    public void createWithCapacity() throws IOException {
        MemoryFileStore store = MemoryFileStore.builder()
                .capacity(100)
                .build();
        assertThat(store.getTotalSpace()).isEqualTo(100);
        assertThat(store.getUsableSpace()).isEqualTo(100);
        assertThat(store.getUnallocatedSpace()).isEqualTo(100);
    }

}
