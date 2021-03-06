// Copyright 2015 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.api.ads.adwords.axis.utils.v201502.shopping;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.api.ads.adwords.axis.v201502.cm.AdGroupCriterion;
import com.google.api.ads.adwords.axis.v201502.cm.BiddableAdGroupCriterion;
import com.google.api.ads.adwords.axis.v201502.cm.BiddingStrategyConfiguration;
import com.google.api.ads.adwords.axis.v201502.cm.Bids;
import com.google.api.ads.adwords.axis.v201502.cm.CpcBid;
import com.google.api.ads.adwords.axis.v201502.cm.Criterion;
import com.google.api.ads.adwords.axis.v201502.cm.NegativeAdGroupCriterion;
import com.google.api.ads.adwords.axis.v201502.cm.ProductPartition;
import com.google.api.ads.adwords.axis.v201502.cm.ProductPartitionType;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link ProductPartitionNodeAdapter}.
 */
@RunWith(JUnit4.class)
public class ProductPartitionNodeAdapterTest {

  private Long adGroupId;
  private BiddingStrategyConfiguration biddingConfig;
  private ProductPartitionNode parentNode;
  private ProductPartitionNode childNode;

  @Rule
  public ExpectedException thrown = ExpectedException.none();
  
  @Before
  public void setUp() throws Exception {
    adGroupId = -1L;
    biddingConfig = new BiddingStrategyConfiguration();
    biddingConfig.setBiddingStrategyId(-2L);
    parentNode = new ProductPartitionNode(null, null, -3L, new ProductDimensionComparator());
    childNode = new ProductPartitionNode(parentNode,
        ProductDimensions.createBrand("google"), -4L,
        new ProductDimensionComparator());
  }

  /**
   * Tests creating a criterion for a REMOVE operation.
   */
  @Test
  public void testCreateCriterionForRemove() {
    testCommonAttributes(childNode,
        ProductPartitionNodeAdapter.createCriterionForRemove(childNode, adGroupId), true);

    testCommonAttributes(parentNode,
        ProductPartitionNodeAdapter.createCriterionForRemove(parentNode, adGroupId), true);
  }

  /**
   * Tests creating a criterion for an ADD operation where the criterion is biddable.
   */
  @Test
  public void testCreateCriterionForAdd_biddableCriterion() {
    childNode.setBid(1500000L);
    testCommonAttributes(childNode,
        ProductPartitionNodeAdapter.createCriterionForAdd(childNode, adGroupId, biddingConfig),
        false);

    parentNode.setBid(1500000L);
    testCommonAttributes(parentNode,
        ProductPartitionNodeAdapter.createCriterionForAdd(parentNode, adGroupId, biddingConfig),
        false);
  }

  /**
   * Tests creating a criterion for an ADD operation where the criterion is excluded.
   */
  @Test
  public void testCreateCriterionForAdd_excludedCriterion() {
    childNode = childNode.asExcludedUnit();
    testCommonAttributes(childNode,
        ProductPartitionNodeAdapter.createCriterionForAdd(childNode, adGroupId, biddingConfig),
        false);
  }

  /**
   * Tests creating a criterion for a SET bid operation where the bid is not null.
   */
  @Test
  public void testCreateCriterionForSetBid_bidNotNull() {
    childNode.setBid(1500000L);
    testCommonAttributes(childNode,
        ProductPartitionNodeAdapter.createCriterionForSetBid(childNode, adGroupId, biddingConfig),
        false);

    parentNode = parentNode.asBiddableUnit().setBid(2000000L);
    testCommonAttributes(parentNode,
        ProductPartitionNodeAdapter.createCriterionForSetBid(parentNode, adGroupId, biddingConfig),
        false);
  }

  /**
   * Tests creating a criterion for a SET bid operation where the bid is null.
   */
  @Test
  public void testCreateCriterionForSetBid_bidNull() {
    childNode.setBid(null);
    testCommonAttributes(childNode,
        ProductPartitionNodeAdapter.createCriterionForSetBid(childNode, adGroupId, biddingConfig),
        false);

    parentNode.setBid(null);
    testCommonAttributes(parentNode,
        ProductPartitionNodeAdapter.createCriterionForSetBid(parentNode, adGroupId, biddingConfig),
        false);
  }

  /**
   * Tests creating a criterion for a SET bid operation for an excluded unit node. This should fail
   * since excluded nodes cannot have bids.
   */
  @Test
  public void testCreateCriterionForSetBid_excludedNode_fails() {
    childNode = childNode.asExcludedUnit();
    thrown.expect(IllegalArgumentException.class);
    ProductPartitionNodeAdapter.createCriterionForSetBid(childNode, adGroupId, biddingConfig);
  }

  /**
   * Asserts that the attributes of {@code adGroupCriterion} match expectations.
   *
   * @param node the node from which the criterion was built
   * @param adGroupCriterion the criterion created by {@link ProductPartitionNodeAdapter}
   * @param isForRemove if true, this method will only check the attributes required for a REMOVE
   *        operation
   */
  private void testCommonAttributes(ProductPartitionNode node, AdGroupCriterion adGroupCriterion,
      boolean isForRemove) {
    assertEquals("Ad group ID is incorrect", adGroupId, adGroupCriterion.getAdGroupId());

    Criterion criterion = adGroupCriterion.getCriterion();
    assertTrue("Criterion should be a ProductPartition", criterion instanceof ProductPartition);

    assertEquals("Partition ID is incorrect", node.getProductPartitionId(), criterion.getId());

    if (isForRemove) {
      assertEquals("Type of AdGroupCriterion for REMOVE should be the base class",
          AdGroupCriterion.class, adGroupCriterion.getClass());

      // The above checks suffice for REMOVE operations.
      return;
    }

    ProductPartition partition = (ProductPartition) criterion;
    assertEquals("The caseValue of the partition does not match the dimension of the node", 0,
        new ProductDimensionComparator().compare(partition.getCaseValue(), node.getDimension()));

    if (node.getParent() == null) {
      assertNull("Parent ID should be null", partition.getParentCriterionId());
    } else {
      assertEquals("Parent ID does not match ID of parent node",
          node.getParent().getProductPartitionId(), partition.getParentCriterionId());
    }

    if (node.isBiddableUnit()) {
      assertTrue("Biddable node should be translated into a BiddableAdGroupCriterion",
          adGroupCriterion instanceof BiddableAdGroupCriterion);
      BiddableAdGroupCriterion biddableCriterion = (BiddableAdGroupCriterion) adGroupCriterion;
      BiddingStrategyConfiguration biddingStrategyConfig =
          biddableCriterion.getBiddingStrategyConfiguration();
      if (node.getBid() == null) {
        assertArrayEquals(new Bids[0], biddingStrategyConfig.getBids());
      } else {
        Bids bid = biddingStrategyConfig.getBids(0);
        assertTrue("Bid should be a CpcBid", bid instanceof CpcBid);
        CpcBid cpcBid = (CpcBid) bid;
        assertEquals("Bid amount is incorrect", node.getBid(), cpcBid.getBid().getMicroAmount());
        assertEquals("Partition is not a UNIT partition", ProductPartitionType.UNIT,
            partition.getPartitionType());
      }
    } else {
      assertTrue("Excluded node should be translated into a NegativeAdGroupCriterion",
          adGroupCriterion instanceof NegativeAdGroupCriterion);

    }
  }
}
