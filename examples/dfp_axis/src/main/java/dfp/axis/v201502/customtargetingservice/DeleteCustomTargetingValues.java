// Copyright 2015 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package dfp.axis.v201502.customtargetingservice;

import com.google.api.ads.common.lib.auth.OfflineCredentials;
import com.google.api.ads.common.lib.auth.OfflineCredentials.Api;
import com.google.api.ads.dfp.axis.factory.DfpServices;
import com.google.api.ads.dfp.axis.utils.v201502.StatementBuilder;
import com.google.api.ads.dfp.axis.v201502.CustomTargetingServiceInterface;
import com.google.api.ads.dfp.axis.v201502.CustomTargetingValue;
import com.google.api.ads.dfp.axis.v201502.CustomTargetingValuePage;
import com.google.api.ads.dfp.axis.v201502.UpdateResult;
import com.google.api.ads.dfp.lib.client.DfpSession;
import com.google.api.client.auth.oauth2.Credential;

/**
 * This example deletes a custom targeting value. To determine which
 * custom targeting keys and values exist, run
 * GetAllCustomTargetingKeysAndValue.java.
 *
 * Credentials and properties in {@code fromFile()} are pulled from the
 * "ads.properties" file. See README for more info.
 */
public class DeleteCustomTargetingValues {

  // Set the ID of the custom targeting value to delete.
  private static final String CUSTOM_TARGETING_VALUE_ID = "INSERT_CUSTOM_TARGETING_VALUE_ID_HERE";

  public static void runExample(DfpServices dfpServices, DfpSession session,
      long customTargetingValueId) throws Exception {
    // Get the CustomTargetingService.
    CustomTargetingServiceInterface customTargetingService =
        dfpServices.get(session, CustomTargetingServiceInterface.class);

    // Create a statement to select custom targeting value.
    StatementBuilder statementBuilder = new StatementBuilder()
        .where("WHERE id = :id")
        .orderBy("id ASC")
        .limit(StatementBuilder.SUGGESTED_PAGE_LIMIT)
        .withBindVariableValue("id", customTargetingValueId);

    // Default for total result set size.
    int totalResultSetSize = 0;

    do {
      // Get custom targeting values by statement.
      CustomTargetingValuePage page = customTargetingService
          .getCustomTargetingValuesByStatement(statementBuilder.toStatement());

      if (page.getResults() != null) {
        totalResultSetSize = page.getTotalResultSetSize();
        int i = page.getStartIndex();
        for (CustomTargetingValue customTargetingValue : page.getResults()) {
          System.out.printf("%d) Custom targeting value with ID \"%d\""
              + " will be deleted.\n", i++, customTargetingValue.getId());
        }
      }

      statementBuilder.increaseOffsetBy(StatementBuilder.SUGGESTED_PAGE_LIMIT);
    } while (statementBuilder.getOffset() < totalResultSetSize);

    System.out.printf("Number of custom targeting values to be deleted: %d\n", totalResultSetSize);

    if (totalResultSetSize > 0) {
      // Remove limit and offset from statement.
      statementBuilder.removeLimitAndOffset();

      // Create action.
      com.google.api.ads.dfp.axis.v201502.DeleteCustomTargetingValues action =
          new com.google.api.ads.dfp.axis.v201502.DeleteCustomTargetingValues();

      // Perform action.
      UpdateResult result = customTargetingService.performCustomTargetingValueAction(
          action, statementBuilder.toStatement());

      if (result != null && result.getNumChanges() > 0) {
        System.out.printf(
            "Number of custom targeting values deleted: %d\n", result.getNumChanges());
      } else {
        System.out.println("No custom targeting values deleted.");
      }
    }
  }

  public static void main(String[] args) throws Exception {
    // Generate a refreshable OAuth2 credential.
    Credential oAuth2Credential = new OfflineCredentials.Builder()
        .forApi(Api.DFP)
        .fromFile()
        .build()
        .generateCredential();

    // Construct a DfpSession.
    DfpSession session = new DfpSession.Builder()
        .fromFile()
        .withOAuth2Credential(oAuth2Credential)
        .build();

    DfpServices dfpServices = new DfpServices();

    runExample(dfpServices, session, Long.parseLong(CUSTOM_TARGETING_VALUE_ID));
  }
}
