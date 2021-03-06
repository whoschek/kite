/*
 * Copyright 2013 Cloudera Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kitesdk.data.spi.filesystem;

import java.net.URI;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericData.Record;
import org.apache.avro.util.Utf8;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kitesdk.data.Dataset;
import org.kitesdk.data.DatasetDescriptor;
import org.kitesdk.data.DatasetNotFoundException;
import org.kitesdk.data.DatasetRepositories;
import org.kitesdk.data.DatasetRepository;
import org.kitesdk.data.Datasets;
import org.kitesdk.data.RefinableView;
import org.kitesdk.data.TestHelpers;
import org.kitesdk.data.spi.Constraints;

public class TestLocalDatasetURIs {

  private static FileSystem localFS;
  private static DatasetDescriptor descriptor;

  @BeforeClass
  public static void createRepositoryAndTestDatasets() throws Exception {
    localFS = FileSystem.getLocal(new Configuration());
    descriptor = new DatasetDescriptor.Builder()
        .schemaUri("resource:schema/user.avsc")
        .build();
  }

  @Test
  public void testAbsolute() {
    DatasetRepository repo = DatasetRepositories.open("repo:file:/tmp/data");
    repo.delete("test");
    repo.create("test", descriptor);

    Dataset<Record> ds = Datasets.<Record, Dataset<Record>>
        load("dataset:file:/tmp/data/test", Record.class);

    Assert.assertNotNull("Should load dataset", ds);
    Assert.assertTrue(ds instanceof FileSystemDataset);
    Assert.assertEquals("Locations should match",
        URI.create("file:/tmp/data/test"),
        ds.getDescriptor().getLocation());
    Assert.assertEquals("Descriptors should match",
        repo.load("test").getDescriptor(), ds.getDescriptor());

    repo.delete("test");
  }

  @Test
  public void testRelative() {
    DatasetRepository repo = DatasetRepositories.open("repo:file:target/data");
    repo.delete("test");
    repo.create("test", descriptor);

    Dataset<Record> ds = Datasets.<Record, Dataset<Record>>
        load("dataset:file:target/data/test", Record.class);

    Assert.assertNotNull("Should load dataset", ds);
    Assert.assertTrue(ds instanceof FileSystemDataset);
    Path cwd = localFS.makeQualified(new Path("."));
    Assert.assertEquals("Locations should match",
        new Path(cwd, "target/data/test").toUri(), ds.getDescriptor().getLocation());
    Assert.assertEquals("Descriptors should match",
        repo.load("test").getDescriptor(), ds.getDescriptor());

    repo.delete("test");
  }

  @Test
  public void testViewConstraints() {
    DatasetRepository repo = DatasetRepositories.open("repo:file:/tmp/data");
    repo.delete("test");
    repo.create("test", descriptor);

    RefinableView<Record> v = Datasets.<Record, RefinableView<Record>>
        load("view:file:/tmp/data/test?username=user", Record.class);

    Assert.assertNotNull("Should load view", v);
    Assert.assertTrue(v instanceof FileSystemView);
    Assert.assertEquals("Locations should match",
        URI.create("file:/tmp/data/test"),
        v.getDataset().getDescriptor().getLocation());

    DatasetDescriptor loaded = repo.load("test").getDescriptor();
    Assert.assertEquals("Descriptors should match",
        loaded, v.getDataset().getDescriptor());

    Constraints withUser = new Constraints(loaded.getSchema())
        .with("username", new Utf8("user"));
    Assert.assertEquals("Constraints should be username=user",
        withUser, ((FileSystemView) v).getConstraints());

    repo.delete("test");
  }

  @Test
  public void testMissingDataset() {
    TestHelpers.assertThrows("Should not find dataset: no such dataset",
        DatasetNotFoundException.class, new Runnable() {
      @Override
      public void run() {
        Dataset<Record> ds = Datasets.<Record, Dataset<Record>>
            load("dataset:file:/tmp/data/nosuchdataset", Record.class);
      }
    });
  }

  @Test
  public void testMissingRepository() {
    TestHelpers.assertThrows("Should not find dataset: unknown storage scheme",
        DatasetNotFoundException.class, new Runnable() {
          @Override
          public void run() {
            Dataset<Record> ds = Datasets.<Record, Dataset<Record>>
                load("dataset:unknown:/tmp/data/test", Record.class);
          }
        });
  }
}
