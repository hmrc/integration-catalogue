/*
 * Copyright 2022 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.integrationcatalogue.testdata

import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.models.info.{Contact, Info}
import io.swagger.v3.oas.models.media.{Content, MediaType}
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.responses.{ApiResponse, ApiResponses}
import io.swagger.v3.oas.models.{OpenAPI, Operation, PathItem, Paths}
import io.swagger.v3.parser.core.models.SwaggerParseResult

import java.util.HashMap
import scala.collection.JavaConverters._
import uk.gov.hmrc.integrationcatalogue.parser.oas.adapters.ExtensionKeys

import java.{util => ju}

trait OasTestData extends ExtensionKeys {
  //*** - OPENAPI STUFF
  val oasApiName = "api-name"
  val oasVersion = "1.0"
  val oasApiDescription = "some description"
  val oasPath1Uri = "/some/Path1"
  val oasPath2Uri = "/some/Path2"
  val oasPath3Uri = "/some/Path3"
  val oasGetEndpointDesc = """ ```Example: /individuals/trn```
        ```
        Change Log

        Version Date        Author         Description
        0.1.0   26-06-2020  John Doe  Initial draft for review
        0.2.0   08-07-2020  John Doe  Adding "INVALID_ORIGINATORID" as a possible 400 error
        1.0.0   30-07-2020  John Doe  Baselined"""

  val oasGetEndpointSummary = "Get endpoint for stuff"
  val oasContactName = "Test Developer"
  val oasContactEMail = "test.developer@hmrc.gov.uk"

  def getSwaggerResult(messages: List[String] = List.empty) = {
    val result = new SwaggerParseResult()
    result.setMessages(messages.asJava)
    result.setOpenAPI(getOpenAPIObject(false))
    result
  }

  def getOpenApiInfo() = {
    val openAPIInfo = new Info()
    openAPIInfo.setTitle(oasApiName)
    openAPIInfo.setDescription(oasApiDescription)
    openAPIInfo.setVersion(oasVersion)
    val contact = new Contact()
    contact.setEmail(oasContactEMail)
    contact.setName(oasContactName)
    openAPIInfo.setContact(contact)
    openAPIInfo
  }

  def getOpenAPIObject(withExtensions: Boolean, backendsExtension: List[String] = List.empty, reviewedDateExtension: Option[String] = None): OpenAPI = {

    val openAPIInfo = getOpenApiInfo()

    if (withExtensions || reviewedDateExtension.isDefined) {
      val sublevelExtensions = new HashMap[String, AnyRef]()

      if (withExtensions) {
        var backends: ju.List[String] = null
        if (Option(backendsExtension).isDefined) {
          backends = new java.util.ArrayList[String]()
          backendsExtension.foreach(e => backends.add(e))
        }
        sublevelExtensions.put(BACKEND_EXTENSION_KEY, backends)
        sublevelExtensions.put(PUBLISHER_REF_EXTENSION_KEY, "self-assessment-api")
      }

      if(reviewedDateExtension.isDefined) {
        sublevelExtensions.put(REVIEWED_DATE_EXTENSION_KEY, reviewedDateExtension.get)
    }
      val topLevelExtensionsMap = new HashMap[String, AnyRef]()
      topLevelExtensionsMap.put(EXTENSIONS_KEY, sublevelExtensions)
      openAPIInfo.setExtensions(topLevelExtensionsMap)

    }

    val requestBody1 = new RequestBody()
    val rbContent1 = new Content
    val content1MediaType = new MediaType()
    val content1Example = new io.swagger.v3.oas.models.examples.Example()

    val responseBodies = new ApiResponses()
    val apiResponse = new ApiResponse()
    apiResponse.setDescription("response description")
    val apiResponseContent1 = new Content()
    val apiResponseMediaType = new MediaType()
    val apiResponseExample = new io.swagger.v3.oas.models.examples.Example()
    apiResponseExample.setValue("{\"SomeRequestValue\" : \"theValue\"}")
    apiResponseExample.setSummary("response summary")
    apiResponseMediaType.addExamples("some example response description", apiResponseExample)
    apiResponseContent1.put("application/json", apiResponseMediaType)
    apiResponse.setContent(apiResponseContent1)

    responseBodies.addApiResponse("200", apiResponse)

    val mapper = new ObjectMapper()
    val jsonNodeVal = mapper.readTree("{\"SomeValue\": \"theValue\"}")
    content1Example.setValue(jsonNodeVal)

    content1MediaType.addExamples("some description", content1Example)
    rbContent1.put("application/json", content1MediaType)
    requestBody1.setContent(rbContent1)
    val pathItem1 = new PathItem()
    val getOperation = new Operation()
    getOperation.setDescription(oasGetEndpointDesc)
    getOperation.setSummary(oasGetEndpointSummary)
    getOperation.setRequestBody(requestBody1)
    getOperation.setResponses(responseBodies)

    pathItem1.setGet(getOperation)
    val pathItem2 = new PathItem()
    pathItem2.setGet(getOperation)
    val pathItem3 = new PathItem()
    pathItem3.setGet(getOperation)
    val pathsObj = new Paths()
    pathsObj.addPathItem(oasPath1Uri, pathItem1)
    pathsObj.addPathItem(oasPath2Uri, pathItem2)
    pathsObj.addPathItem(oasPath3Uri, pathItem3)
    val openApiObj = new OpenAPI()
    openApiObj.setInfo(openAPIInfo)
    openApiObj.setPaths(pathsObj)

    openApiObj
  }

  val rawOASDataWithExtensions = raw"""openapi: 3.0.3
                                      |info:
                                      |  title: '$oasApiName'
                                      |  description: >-
                                      |    $oasApiDescription
                                      |  version: 1.0
                                      |  contact:
                                      |    name: Test Developer
                                      |    email: test.developer@hmrc.gov.uk
                                      |  x-integration-catalogue:
                                      |    reviewed-date: 2020-12-25
                                      |    short-description: "I am a short description"
                                      |    backends:
                                      |      - ITMP
                                      |      - NPS
                                      |servers:
                                      |  - url: 'https://{hostname}:{port}'
                                      |    description: >-
                                      |      Actual environment values can be obtained from IF platforms team for each
                                      |      environment
                                      |tags:
                                      |  - name: 'API#10000'
                                      |    description: Get Data
                                      |paths:
                                      |  '/individuals/state-pensions/{idType}/{idValue}/summary':
                                      |    get:
                                      |      summary: 'API#10000 Get Data'
                                      |      description: some description
                                      |      operationId: getStatePensionSummary
                                      |      tags:
                                      |        - 'API#10000'
                                      |      security:
                                      |        - bearerAuth: []
                                      |      parameters:
                                      |        - $$ref: '#/components/parameters/environment'
                                      |        - $$ref: '#/components/parameters/correlationId'
                                      |        - $$ref: '#/components/parameters/originatorId'
                                      |        - $$ref: '#/components/parameters/idTypeParam'
                                      |        - $$ref: '#/components/parameters/idValueParam'
                                      |      responses:
                                      |        '200':
                                      |          description: Successful Response
                                      |          headers:
                                      |            CorrelationId:
                                      |              $$ref: '#/components/headers/CorrelationId'
                                      |          content:
                                      |            application/json;charset=UTF-8:
                                      |              schema:
                                      |                $$ref: '#/components/schemas/successResponse'
                                      |              examples:
                                      |                Example1:
                                      |                  summary: Success response
                                      |                  value:
                                      |                    nino: AA123000
                                      |                    accountNotMaintainedFlag: true
                                      |                    addressPostcode: 'TF3 4NT'
                                      |                    contractedOutFlag: 1
                                      |                    countryCode: 123
                                      |                    dateOfBirth: '1945-03-22'
                                      |                    dateOfDeath: '2019-04-29'
                                      |                    earningsIncludedUpto: '2016-08-25'
                                      |                    finalRelevantYear: 2016
                                      |                    manualCorrespondenceIndicator: true
                                      |                    minimumQualifyingPeriod: true
                                      |                    nspQualifyingYears: 3
                                      |                    nspRequisiteYears: 6
                                      |                    pensionShareOrderCoeg: true
                                      |                    pensionShareOrderSerps: true
                                      |                    reducedRateElectionToConsider: true
                                      |                    sensitiveCaseFlag: 1
                                      |                    sex: M
                                      |                    spaDate: '2015-03-22'
                                      |                    pensionForecast:
                                      |                      forecastAmount: 2345.99
                                      |                      forecastAmount2016: 4533.99
                                      |                      nspMax: 3453.99
                                      |                      qualifyingYearsAtSpa: 2
                                      |                    statePensionAmount:
                                      |                      apAmount: 45646.99
                                      |                      amountA2016:
                                      |                        grbCash: 35435.99
                                      |                        ltbCatACashValue: 5353.99
                                      |                        ltbPost02ApCashValue: 34533.99
                                      |                        ltbPost88CodCashValue: 3533.99
                                      |                        ltbPost97ApCashValue: 3455.99
                                      |                        ltbPre88CodCashValue: 5646.99
                                      |                        ltbPre97ApCashValue: 4664.99
                                      |                        ltbPst88GmpCashValue: 4566.99
                                      |                        pre88Gmp: 7646.99
                                      |                      amountB2016:
                                      |                        mainComponent: 4564.99
                                      |                        rebateDerivedAmount: 4654.99
                                      |                      nspEntitlement: 4564.99
                                      |                      protectedPayment2016: 4646.99
                                      |                      startingAmount: 4564.99
                                      |        '400':
                                      |          description: >-
                                      |            Bad request
                                      |
                                      |            ```
                                      |
                                      |            A bad request has been made; this could be due to one or more issues
                                      |            with the request
                                      |
                                      |            "code"                  "reason"
                                      |
                                      |            INVALID_IDTYPE          Submission has not passed validation. Invalid parameter idType.
                                      |
                                      |            INVALID_IDVALUE         Submission has not passed validation. Invalid parameter idValue.
                                      |
                                      |            INVALID_ORIGINATOR_ID   Submission has not passed validation. Invalid Header parameter OriginatorId.
                                      |
                                      |            INVALID_CORRELATIONID   Submission has not passed validation. Invalid Header parameter CorrelationId.
                                      |          headers:
                                      |            CorrelationId:
                                      |              $$ref: '#/components/headers/CorrelationId'
                                      |          content:
                                      |            application/json;charset=UTF-8:
                                      |              schema:
                                      |                $$ref: '#/components/schemas/errorResponse'
                                      |              examples:
                                      |                Example1_SingleCode:
                                      |                  summary: Single Error Code
                                      |                  value:
                                      |                    failures:
                                      |                      - code: INVALID_IDTYPE
                                      |                        reason: >-
                                      |                          Submission has not passed validation. Invalid parameter idType.
                                      |                Example2_MultipleErrorCodes:
                                      |                  summary: Multiple Error Codes
                                      |                  value:
                                      |                    failures:
                                      |                      - code: INVALID_ORIGINATOR_ID
                                      |                        reason: >-
                                      |                          Submission has not passed validation. Invalid Header parameter OriginatorId.
                                      |                      - code: INVALID_CORRELATIONID
                                      |                        reason: >-
                                      |                          Submission has not passed validation. Invalid Header parameter CorrelationId.
                                      |        '404':
                                      |          description: >-
                                      |            Not Found
                                      |
                                      |            ```
                                      |
                                      |            "code"                        "reason"
                                      |
                                      |            NO_DATA_FOUND                 The remote endpoint has indicated that
                                      |            no data can be found for the nino.
                                      |          headers:
                                      |            CorrelationId:
                                      |              $$ref: '#/components/headers/CorrelationId'
                                      |          content:
                                      |            application/json;charset=UTF-8:
                                      |              schema:
                                      |                $$ref: '#/components/schemas/errorResponse'
                                      |              examples:
                                      |                Example-NO_DATA_FOUND:
                                      |                  value:
                                      |                    failures:
                                      |                      - code: NO_DATA_FOUND
                                      |                        reason: >-
                                      |                          The remote endpoint has indicated that no data can be found for the nino.
                                      |        '500':
                                      |          description: >-
                                      |            Server Error
                                      |
                                      |            ```
                                      |
                                      |            "code"         "reason"
                                      |
                                      |            SERVER_ERROR   IF is currently experiencing problems that require live service intervention.
                                      |          headers:
                                      |            CorrelationId:
                                      |              $$ref: '#/components/headers/CorrelationId'
                                      |          content:
                                      |            application/json;charset=UTF-8:
                                      |              schema:
                                      |                $$ref: '#/components/schemas/errorResponse'
                                      |              examples:
                                      |                Example-ServerError:
                                      |                  value:
                                      |                    failures:
                                      |                      - code: SERVER_ERROR
                                      |                        reason: >-
                                      |                          IF is currently experiencing problems that require live service intervention.
                                      |        '503':
                                      |          description: >-
                                      |            Service unavailable
                                      |
                                      |            ```
                                      |
                                      |            "code"                "reason"
                                      |
                                      |            SERVICE_UNAVAILABLE   Dependent systems are currently not responding.
                                      |          headers:
                                      |            CorrelationId:
                                      |              $$ref: '#/components/headers/CorrelationId'
                                      |          content:
                                      |            application/json;charset=UTF-8:
                                      |              schema:
                                      |                $$ref: '#/components/schemas/errorResponse'
                                      |              examples:
                                      |                Example-ServerUnavailable:
                                      |                  value:
                                      |                    failures:
                                      |                      - code: SERVICE_UNAVAILABLE
                                      |                        reason: Dependent systems are currently not responding.
                                      |components:
                                      |  securitySchemes:
                                      |    bearerAuth:
                                      |      type: http
                                      |      scheme: bearer
                                      |  headers:
                                      |    CorrelationId:
                                      |      description: CorrelationID - Used for traceability purposes when present
                                      |      schema:
                                      |        type: string
                                      |        pattern: >-
                                      |          ^[0-9a-fA-F]{8}[-][0-9a-fA-F]{4}[-][0-9a-fA-F]{4}[-][0-9a-fA-F]{4}[-][0-9a-fA-F]{12}$$
                                      |  parameters:
                                      |    environment:
                                      |      in: header
                                      |      name: Environment
                                      |      description: Mandatory. The environment in use.
                                      |      required: true
                                      |      schema:
                                      |        type: string
                                      |        enum:
                                      |          - ist0
                                      |          - clone
                                      |          - live
                                      |    correlationId:
                                      |      in: header
                                      |      name: CorrelationId
                                      |      description: >-
                                      |        Optional. A UUID format string for the transaction. If not specified the
                                      |        IF will create a UUID value to be used
                                      |      required: false
                                      |      schema:
                                      |        type: string
                                      |        pattern: >-
                                      |          ^[0-9a-fA-F]{8}[-][0-9a-fA-F]{4}[-][0-9a-fA-F]{4}[-][0-9a-fA-F]{4}[-][0-9a-fA-F]{12}$$
                                      |    originatorId:
                                      |      in: header
                                      |      name: OriginatorId
                                      |      description: Mandatory.The originator id
                                      |      required: true
                                      |      schema:
                                      |        type: string
                                      |        pattern: '^DA2_PF$$'
                                      |    idTypeParam:
                                      |      in: path
                                      |      name: idType
                                      |      description: Identification type. Possible value nino
                                      |      required: true
                                      |      schema:
                                      |        type: string
                                      |        pattern: '^nino$$'
                                      |    idValueParam:
                                      |      in: path
                                      |      name: idValue
                                      |      description: idValue for the idType nino - National Insurance number with or without suffix
                                      |      required: true
                                      |      schema:
                                      |        type: string
                                      |        pattern: >-
                                      |          ^((?!(BG|GB|KN|NK|NT|TN|ZZ)|(D|F|I|Q|U|V)[A-Z]|[A-Z](D|F|I|O|Q|U|V))[A-Z]{2})[0-9]{6}[A-D]?$$
                                      |  schemas:
                                      |    errorResponse:
                                      |      title: 'API#10000 Get Data Error Response Schema'
                                      |      type: object
                                      |      additionalProperties: false
                                      |      required:
                                      |        - failures
                                      |      properties:
                                      |        failures:
                                      |          type: array
                                      |          minItems: 1
                                      |          uniqueItems: true
                                      |          items:
                                      |            type: object
                                      |            additionalProperties: false
                                      |            required:
                                      |              - code
                                      |              - reason
                                      |            properties:
                                      |              code:
                                      |                description: Keys for all the errors returned
                                      |                type: string
                                      |                pattern: '^[A-Z0-9_-]{1,160}$$'
                                      |              reason:
                                      |                description: A simple description for the failure
                                      |                type: string
                                      |                minLength: 1
                                      |                maxLength: 160
                                      |    successResponse:
                                      |      title: 'API#1000 Get Data Success Response Schema'
                                      |      type: object
                                      |      properties:
                                      |        nino:
                                      |          description: >-
                                      |            The National Insurance Number, without a suffix, to which the
                                      |            pension data belongs
                                      |          type: string
                                      |          pattern: >-
                                      |            ^((?!(BG|GB|KN|NK|NT|TN|ZZ)|(D|F|I|Q|U|V)[A-Z]|[A-Z](D|F|I|O|Q|U|V))[A-Z]{2})[0-9]{6}$$
                                      |        accountNotMaintainedFlag:
                                      |          description: >-
                                      |            true denotes that the account has not been maintained on NPS, false
                                      |            denotes that the account has been maintained on NPS
                                      |          type: boolean
                                      |        addressPostcode:
                                      |          type: string
                                      |          pattern: '^(([A-Z]{1,2}[0-9][0-9A-Z]? [0-9][A-Z]{2})|(BFPO ?[0-9]{1,4}))$$'
                                      |        contractedOutFlag:
                                      |          description: >-
                                      |            0 denotes that the person is not contracted out; 1 denotes that the
                                      |            person is a member of a COSR, COMP or COMB scheme; 2 denotes that
                                      |            the person is a member of a PP scheme
                                      |          type: integer
                                      |          minimum: 0
                                      |          maximum: 2
                                      |        countryCode:
                                      |          description: >-
                                      |            NPS Address Country code
                                      |          type: integer
                                      |          minimum: 0
                                      |          maximum: 999
                                      |        dateOfBirth:
                                      |          description: The date of birth of the insured person. format(CCYY-MM-DD)
                                      |          type: string
                                      |          minLength: 10
                                      |          maxLength: 10
                                      |        dateOfDeath:
                                      |          description: The date of death of the insured person. format(CCYY-MM-DD)
                                      |          type: string
                                      |          minLength: 10
                                      |          maxLength: 10
                                      |        earningsIncludedUpto:
                                      |          description: The last Tax Year that was included in the Tax Calculation. format(CCYY-MM-DD)
                                      |          type: string
                                      |          minLength: 10
                                      |          maxLength: 10
                                      |        finalRelevantYear:
                                      |          type: integer
                                      |          description: >-
                                      |            The Final Relevant Year is the last complete tax year before death
                                      |            or pension age (whichever is earlier)
                                      |          minimum: 1900
                                      |          maximum: 2099
                                      |        manualCorrespondenceIndicator:
                                      |          description: Manual Correspondence Indicator
                                      |          type: boolean
                                      |        minimumQualifyingPeriod:
                                      |          description: >-
                                      |            A flag set to indicate whether the insured person has the minimum
                                      |            number of qualifying periods for the New State Pension
                                      |          type: boolean
                                      |        nspQualifyingYears:
                                      |          description: The number of qualifying years earned to date for New State Pension
                                      |          type: integer
                                      |          minimum: 0
                                      |          maximum: 100
                                      |        nspRequisiteYears:
                                      |          description: The maximum number of New State Pension Qualifying Years allowed
                                      |          type: integer
                                      |          minimum: 0
                                      |          maximum: 100
                                      |        pensionShareOrderCoeg:
                                      |          description: >-
                                      |            A flag set to indicate whether the insured person is subject to a
                                      |            Pension Share Order (COEG); false - they are not, true - they are
                                      |          type: boolean
                                      |        pensionShareOrderSerps:
                                      |          description: >-
                                      |            A flag set to indicate whether the insured person is subject to a
                                      |            Pension Share Order (SERPS); false - they are not, true - they are
                                      |          type: boolean
                                      |        reducedRateElectionToConsider:
                                      |          description: >-
                                      |            A flag set to indicate whether there was a Reduced Rate Election in
                                      |            force at the beginning of the tax year 35 years before the tax year
                                      |            of SPA; false - there was not, true - there was
                                      |          type: boolean
                                      |        sensitiveCaseFlag:
                                      |          description: >-
                                      |            A nationally set indicator on a computer account which indicates the
                                      |            nature of sensitivity and inhibits access to the account except for
                                      |            person(s) with the required level of authority: 0 - NON SENSITIVE, 1
                                      |            - TRANSSEXUAL, 2 - VIP, 3 - OTHER, 9- SPECIAL MONITOR
                                      |          type: integer
                                      |          minimum: 0
                                      |          maximum: 9
                                      |        sex:
                                      |          type: string
                                      |          enum:
                                      |            - M
                                      |            - F
                                      |            - U
                                      |        spaDate:
                                      |          description: The State Pension Age of the insured person. format(CCYY-MM-DD)
                                      |          type: string
                                      |          minLength: 10
                                      |          maxLength: 10
                                      |        pensionForecast:
                                      |          type: object
                                      |          properties:
                                      |            forecastAmount:
                                      |              description: Forecast weekly amount at State Pension Age
                                      |              type: number
                                      |              minimum: -9999999999999.99
                                      |              maximum: 9999999999999.99
                                      |              multipleOf: 0.01
                                      |            forecastAmount2016:
                                      |              type: number
                                      |              minimum: -9999999999999.99
                                      |              maximum:  9999999999999.99
                                      |              multipleOf: 0.01
                                      |            nspMax:
                                      |              description: Maximum amount of New State Pension
                                      |              type: number
                                      |              minimum: -9999999999999.99
                                      |              maximum:  9999999999999.99
                                      |              multipleOf: 0.01
                                      |            qualifyingYearsAtSpa:
                                      |              description: >-
                                      |                The number of qualifying years the person will have if they
                                      |                continue to contribute at the current rate at State Pension Age
                                      |              type: integer
                                      |              minimum: 0
                                      |              maximum: 99
                                      |          additionalProperties: false
                                      |        statePensionAmount:
                                      |          type: object
                                      |          properties:
                                      |            apAmount:
                                      |              description: >-
                                      |                The amount of AP accrued by the person for the last fully posted
                                      |                tax year.
                                      |              type: number
                                      |              minimum: -9999999999999.99
                                      |              maximum:  9999999999999.99
                                      |              multipleOf: 0.01
                                      |            amountA2016:
                                      |              description: Pre 2016 Amount A Pension Details
                                      |              type: object
                                      |              properties:
                                      |                grbCash:
                                      |                  description: The current cash value of the Graduated Retirement Benefit
                                      |                  type: number
                                      |                  minimum: -9999999999999.99
                                      |                  maximum:  9999999999999.99
                                      |                  multipleOf: 0.01
                                      |                ltbCatACashValue:
                                      |                  description: The pre 2016 Old Rules Basic Pension amount
                                      |                  type: number
                                      |                  minimum: -9999999999999.99
                                      |                  maximum: 9999999999999.99
                                      |                  multipleOf: 0.01
                                      |                ltbPost02ApCashValue:
                                      |                  description: >-
                                      |                    The amount of additional pension accrued between 2002 and
                                      |                    2016
                                      |                  type: number
                                      |                  minimum: -9999999999999.99
                                      |                  maximum:  9999999999999.99
                                      |                  multipleOf: 0.01
                                      |                ltbPost88CodCashValue:
                                      |                  description: >-
                                      |                    The current cash value of the Contracted Out Deduction from
                                      |                    1988 onwards
                                      |                  type: number
                                      |                  minimum: -9999999999999.99
                                      |                  maximum:  9999999999999.99
                                      |                  multipleOf: 0.01
                                      |                ltbPost97ApCashValue:
                                      |                  description: >-
                                      |                    The amount of additional pension accrued between 1997 and
                                      |                    2002
                                      |                  type: number
                                      |                  minimum:  -9999999999999.99
                                      |                  maximum:  9999999999999.99
                                      |                  multipleOf: 0.01
                                      |                ltbPre88CodCashValue:
                                      |                  description: >-
                                      |                    The current cash value of the Contracted Out Deduction pre
                                      |                    1988
                                      |                  type: number
                                      |                  minimum: -9999999999999.99
                                      |                  maximum:  9999999999999.99
                                      |                  multipleOf: 0.01
                                      |                ltbPre97ApCashValue:
                                      |                  description: >-
                                      |                    The pre Pre-97 pension amount before the contracted out
                                      |                    deduction has taken place
                                      |                  type: number
                                      |                  minimum: -9999999999999.99
                                      |                  maximum:  9999999999999.99
                                      |                  multipleOf: 0.01
                                      |                ltbPst88GmpCashValue:
                                      |                  description: >-
                                      |                    The current cash value of the Guaranteed Minimum Pension
                                      |                    from 1988 onwards
                                      |                  type: number
                                      |                  minimum: -9999999999999.99
                                      |                  maximum:  9999999999999.99
                                      |                  multipleOf: 0.01
                                      |                pre88Gmp:
                                      |                  description: The cash value of the Guaranteed Minimum Pension pre 1988
                                      |                  type: number
                                      |                  minimum: -9999999999999.99
                                      |                  maximum:  9999999999999.99
                                      |                  multipleOf: 0.01
                                      |              additionalProperties: false
                                      |            amountB2016:
                                      |              description: Pre 2016 Amount B Pension Details
                                      |              type: object
                                      |              properties:
                                      |                mainComponent:
                                      |                  description: >-
                                      |                    The New State Pension amount for the insured person at or
                                      |                    before 2016 before the Rebate Derived Amount has been
                                      |                    deducted
                                      |                  type: number
                                      |                  minimum: -9999999999999.99
                                      |                  maximum:  9999999999999.99
                                      |                  multipleOf: 0.01
                                      |                rebateDerivedAmount:
                                      |                  description: >-
                                      |                    Adjustment to the New State Pension amount to take account
                                      |                    of periods of contracting out
                                      |                  type: number
                                      |                  minimum: -9999999999999.99
                                      |                  maximum:  9999999999999.99
                                      |                  multipleOf: 0.01
                                      |              additionalProperties: false
                                      |            nspEntitlement:
                                      |              description: >-
                                      |                The current value of New State Pension for the insured person at
                                      |                the time of calculation
                                      |              type: number
                                      |              minimum: -9999999999999.99
                                      |              maximum:  9999999999999.99
                                      |              multipleOf: 0.01
                                      |            protectedPayment2016:
                                      |              description: The Protected Payment for the insured person at or before 2016
                                      |              type: number
                                      |              minimum: -9999999999999.99
                                      |              maximum:  9999999999999.99
                                      |              multipleOf: 0.01
                                      |            startingAmount:
                                      |              description: The Starting Amount for the insured person at or before 2016
                                      |              type: number
                                      |              minimum: -9999999999999.99
                                      |              maximum:  9999999999999.99
                                      |              multipleOf: 0.01
                                      |          additionalProperties: false
                                      |      additionalProperties: false
                                      |""".stripMargin

  def rawOASData(contactName: String) = raw"""openapi: 3.0.3
                        |info:
                        |  title: '$oasApiName'
                        |  description: >-
                        |    $oasApiDescription
                        |  version: 1.0
                        |  contact:
                        |    name: $contactName
                        |    email: test.developer@hmrc.gov.uk
                        |  x-integration-catalogue:
                        |    reviewed-date: 2020-12-25
                        |servers:
                        |  - url: 'https://{hostname}:{port}'
                        |    description: >-
                        |      Actual environment values can be obtained from IF platforms team for each
                        |      environment
                        |tags:
                        |  - name: 'API#10000'
                        |    description: Get Data
                        |paths:
                        |  '/individuals/state-pensions/{idType}/{idValue}/summary':
                        |    get:
                        |      summary: 'API#10000 Get Data'
                        |      description: >
                        |        Retrieves State Pension summary data using idType and idValue  <br>
                        |
                        |        /individuals/state-pensions/nino/AB123456C/summary <br>
                        |
                        |        Accepts idType nino and idValue(ninoValue) with or without the single suffix character.<br> NOTE - The backend interface does not accept a National Insurance number
                        |        with a suffix therefore IF must strip this off, if provided, for the
                        |        the call to the backend.
                        |
                        |        ```
                        |
                        |        Change Log
                        |
                        |        ```
                        |
                        |          | Version | Date | Author | Description |
                        |          |---|-----|------|-----|
                        |          | 0.1.0   | 09-10-2020 | John Doe | Initial draft |
                        |          | 1.0.0   | 19-10-2020 | John Doe | Baselined. Postcode regEx matched to backend spec. |
                        |          |||| - nino is made mandatory in response schema. Request header Originator-Id changed to OriginatorId. |
                        |
                        |      operationId: getStatePensionSummary
                        |      tags:
                        |        - 'API#10000'
                        |      security:
                        |        - bearerAuth: []
                        |      parameters:
                        |        - $$ref: '#/components/parameters/environment'
                        |        - $$ref: '#/components/parameters/correlationId'
                        |        - $$ref: '#/components/parameters/originatorId'
                        |        - $$ref: '#/components/parameters/idTypeParam'
                        |        - $$ref: '#/components/parameters/idValueParam'
                        |      responses:
                        |        '200':
                        |          description: Successful Response
                        |          headers:
                        |            CorrelationId:
                        |              $$ref: '#/components/headers/CorrelationId'
                        |          content:
                        |            application/json;charset=UTF-8:
                        |              schema:
                        |                $$ref: '#/components/schemas/successResponse'
                        |              examples:
                        |                Example1:
                        |                  summary: Success response
                        |                  value:
                        |                    nino: AA123000
                        |                    accountNotMaintainedFlag: true
                        |                    addressPostcode: 'TF3 4NT'
                        |                    contractedOutFlag: 1
                        |                    countryCode: 123
                        |                    dateOfBirth: '1945-03-22'
                        |                    dateOfDeath: '2019-04-29'
                        |                    earningsIncludedUpto: '2016-08-25'
                        |                    finalRelevantYear: 2016
                        |                    manualCorrespondenceIndicator: true
                        |                    minimumQualifyingPeriod: true
                        |                    nspQualifyingYears: 3
                        |                    nspRequisiteYears: 6
                        |                    pensionShareOrderCoeg: true
                        |                    pensionShareOrderSerps: true
                        |                    reducedRateElectionToConsider: true
                        |                    sensitiveCaseFlag: 1
                        |                    sex: M
                        |                    spaDate: '2015-03-22'
                        |                    pensionForecast:
                        |                      forecastAmount: 2345.99
                        |                      forecastAmount2016: 4533.99
                        |                      nspMax: 3453.99
                        |                      qualifyingYearsAtSpa: 2
                        |                    statePensionAmount:
                        |                      apAmount: 45646.99
                        |                      amountA2016:
                        |                        grbCash: 35435.99
                        |                        ltbCatACashValue: 5353.99
                        |                        ltbPost02ApCashValue: 34533.99
                        |                        ltbPost88CodCashValue: 3533.99
                        |                        ltbPost97ApCashValue: 3455.99
                        |                        ltbPre88CodCashValue: 5646.99
                        |                        ltbPre97ApCashValue: 4664.99
                        |                        ltbPst88GmpCashValue: 4566.99
                        |                        pre88Gmp: 7646.99
                        |                      amountB2016:
                        |                        mainComponent: 4564.99
                        |                        rebateDerivedAmount: 4654.99
                        |                      nspEntitlement: 4564.99
                        |                      protectedPayment2016: 4646.99
                        |                      startingAmount: 4564.99
                        |        '400':
                        |          description: >-
                        |            Bad request
                        |
                        |            ```
                        |
                        |            A bad request has been made; this could be due to one or more issues
                        |            with the request
                        |
                        |            "code"                  "reason"
                        |
                        |            INVALID_IDTYPE          Submission has not passed validation. Invalid parameter idType.
                        |
                        |            INVALID_IDVALUE         Submission has not passed validation. Invalid parameter idValue.
                        |
                        |            INVALID_ORIGINATOR_ID   Submission has not passed validation. Invalid Header parameter OriginatorId.
                        |
                        |            INVALID_CORRELATIONID   Submission has not passed validation. Invalid Header parameter CorrelationId.
                        |          headers:
                        |            CorrelationId:
                        |              $$ref: '#/components/headers/CorrelationId'
                        |          content:
                        |            application/json;charset=UTF-8:
                        |              schema:
                        |                $$ref: '#/components/schemas/errorResponse'
                        |              examples:
                        |                Example1_SingleCode:
                        |                  summary: Single Error Code
                        |                  value:
                        |                    failures:
                        |                      - code: INVALID_IDTYPE
                        |                        reason: >-
                        |                          Submission has not passed validation. Invalid parameter idType.
                        |                Example2_MultipleErrorCodes:
                        |                  summary: Multiple Error Codes
                        |                  value:
                        |                    failures:
                        |                      - code: INVALID_ORIGINATOR_ID
                        |                        reason: >-
                        |                          Submission has not passed validation. Invalid Header parameter OriginatorId.
                        |                      - code: INVALID_CORRELATIONID
                        |                        reason: >-
                        |                          Submission has not passed validation. Invalid Header parameter CorrelationId.
                        |        '404':
                        |          description: >-
                        |            Not Found
                        |
                        |            ```
                        |
                        |            "code"                        "reason"
                        |
                        |            NO_DATA_FOUND                 The remote endpoint has indicated that
                        |            no data can be found for the nino.
                        |          headers:
                        |            CorrelationId:
                        |              $$ref: '#/components/headers/CorrelationId'
                        |          content:
                        |            application/json;charset=UTF-8:
                        |              schema:
                        |                $$ref: '#/components/schemas/errorResponse'
                        |              examples:
                        |                Example-NO_DATA_FOUND:
                        |                  value:
                        |                    failures:
                        |                      - code: NO_DATA_FOUND
                        |                        reason: >-
                        |                          The remote endpoint has indicated that no data can be found for the nino.
                        |        '500':
                        |          description: >-
                        |            Server Error
                        |
                        |            ```
                        |
                        |            "code"         "reason"
                        |
                        |            SERVER_ERROR   IF is currently experiencing problems that require live service intervention.
                        |          headers:
                        |            CorrelationId:
                        |              $$ref: '#/components/headers/CorrelationId'
                        |          content:
                        |            application/json;charset=UTF-8:
                        |              schema:
                        |                $$ref: '#/components/schemas/errorResponse'
                        |              examples:
                        |                Example-ServerError:
                        |                  value:
                        |                    failures:
                        |                      - code: SERVER_ERROR
                        |                        reason: >-
                        |                          IF is currently experiencing problems that require live service intervention.
                        |        '503':
                        |          description: >-
                        |            Service unavailable
                        |
                        |            ```
                        |
                        |            "code"                "reason"
                        |
                        |            SERVICE_UNAVAILABLE   Dependent systems are currently not responding.
                        |          headers:
                        |            CorrelationId:
                        |              $$ref: '#/components/headers/CorrelationId'
                        |          content:
                        |            application/json;charset=UTF-8:
                        |              schema:
                        |                $$ref: '#/components/schemas/errorResponse'
                        |              examples:
                        |                Example-ServerUnavailable:
                        |                  value:
                        |                    failures:
                        |                      - code: SERVICE_UNAVAILABLE
                        |                        reason: Dependent systems are currently not responding.
                        |components:
                        |  securitySchemes:
                        |    bearerAuth:
                        |      type: http
                        |      scheme: bearer
                        |  headers:
                        |    CorrelationId:
                        |      description: CorrelationID - Used for traceability purposes when present
                        |      schema:
                        |        type: string
                        |        pattern: >-
                        |          ^[0-9a-fA-F]{8}[-][0-9a-fA-F]{4}[-][0-9a-fA-F]{4}[-][0-9a-fA-F]{4}[-][0-9a-fA-F]{12}$$
                        |  parameters:
                        |    environment:
                        |      in: header
                        |      name: Environment
                        |      description: Mandatory. The environment in use.
                        |      required: true
                        |      schema:
                        |        type: string
                        |        enum:
                        |          - ist0
                        |          - clone
                        |          - live
                        |    correlationId:
                        |      in: header
                        |      name: CorrelationId
                        |      description: >-
                        |        Optional. A UUID format string for the transaction. If not specified the
                        |        IF will create a UUID value to be used
                        |      required: false
                        |      schema:
                        |        type: string
                        |        pattern: >-
                        |          ^[0-9a-fA-F]{8}[-][0-9a-fA-F]{4}[-][0-9a-fA-F]{4}[-][0-9a-fA-F]{4}[-][0-9a-fA-F]{12}$$
                        |    originatorId:
                        |      in: header
                        |      name: OriginatorId
                        |      description: Mandatory.The originator id
                        |      required: true
                        |      schema:
                        |        type: string
                        |        pattern: '^DA2_PF$$'
                        |    idTypeParam:
                        |      in: path
                        |      name: idType
                        |      description: Identification type. Possible value nino
                        |      required: true
                        |      schema:
                        |        type: string
                        |        pattern: '^nino$$'
                        |    idValueParam:
                        |      in: path
                        |      name: idValue
                        |      description: idValue for the idType nino - National Insurance number with or without suffix
                        |      required: true
                        |      schema:
                        |        type: string
                        |        pattern: >-
                        |          ^((?!(BG|GB|KN|NK|NT|TN|ZZ)|(D|F|I|Q|U|V)[A-Z]|[A-Z](D|F|I|O|Q|U|V))[A-Z]{2})[0-9]{6}[A-D]?$$
                        |  schemas:
                        |    errorResponse:
                        |      title: 'API#10000 Get Data Error Response Schema'
                        |      type: object
                        |      additionalProperties: false
                        |      required:
                        |        - failures
                        |      properties:
                        |        failures:
                        |          type: array
                        |          minItems: 1
                        |          uniqueItems: true
                        |          items:
                        |            type: object
                        |            additionalProperties: false
                        |            required:
                        |              - code
                        |              - reason
                        |            properties:
                        |              code:
                        |                description: Keys for all the errors returned
                        |                type: string
                        |                pattern: '^[A-Z0-9_-]{1,160}$$'
                        |              reason:
                        |                description: A simple description for the failure
                        |                type: string
                        |                minLength: 1
                        |                maxLength: 160
                        |    successResponse:
                        |      title: 'API#10000 Get Data Success Response Schema'
                        |      type: object
                        |      properties:
                        |        nino:
                        |          description: >-
                        |            The National Insurance Number, without a suffix, to which the
                        |            pension data belongs
                        |          type: string
                        |          pattern: >-
                        |            ^((?!(BG|GB|KN|NK|NT|TN|ZZ)|(D|F|I|Q|U|V)[A-Z]|[A-Z](D|F|I|O|Q|U|V))[A-Z]{2})[0-9]{6}$$
                        |        accountNotMaintainedFlag:
                        |          description: >-
                        |            true denotes that the account has not been maintained on NPS, false
                        |            denotes that the account has been maintained on NPS
                        |          type: boolean
                        |        addressPostcode:
                        |          type: string
                        |          pattern: '^(([A-Z]{1,2}[0-9][0-9A-Z]? [0-9][A-Z]{2})|(BFPO ?[0-9]{1,4}))$$'
                        |        contractedOutFlag:
                        |          description: >-
                        |            0 denotes that the person is not contracted out; 1 denotes that the
                        |            person is a member of a COSR, COMP or COMB scheme; 2 denotes that
                        |            the person is a member of a PP scheme
                        |          type: integer
                        |          minimum: 0
                        |          maximum: 2
                        |        countryCode:
                        |          description: >-
                        |            NPS Address Country code
                        |          type: integer
                        |          minimum: 0
                        |          maximum: 999
                        |        dateOfBirth:
                        |          description: The date of birth of the insured person. format(CCYY-MM-DD)
                        |          type: string
                        |          minLength: 10
                        |          maxLength: 10
                        |        dateOfDeath:
                        |          description: The date of death of the insured person. format(CCYY-MM-DD)
                        |          type: string
                        |          minLength: 10
                        |          maxLength: 10
                        |        earningsIncludedUpto:
                        |          description: The last Tax Year that was included in the Tax Calculation. format(CCYY-MM-DD)
                        |          type: string
                        |          minLength: 10
                        |          maxLength: 10
                        |        finalRelevantYear:
                        |          type: integer
                        |          description: >-
                        |            The Final Relevant Year is the last complete tax year before death
                        |            or pension age (whichever is earlier)
                        |          minimum: 1900
                        |          maximum: 2099
                        |        manualCorrespondenceIndicator:
                        |          description: Manual Correspondence Indicator
                        |          type: boolean
                        |        minimumQualifyingPeriod:
                        |          description: >-
                        |            A flag set to indicate whether the insured person has the minimum
                        |            number of qualifying periods for the New State Pension
                        |          type: boolean
                        |        nspQualifyingYears:
                        |          description: The number of qualifying years earned to date for New State Pension
                        |          type: integer
                        |          minimum: 0
                        |          maximum: 100
                        |        nspRequisiteYears:
                        |          description: The maximum number of New State Pension Qualifying Years allowed
                        |          type: integer
                        |          minimum: 0
                        |          maximum: 100
                        |        pensionShareOrderCoeg:
                        |          description: >-
                        |            A flag set to indicate whether the insured person is subject to a
                        |            Pension Share Order (COEG); false - they are not, true - they are
                        |          type: boolean
                        |        pensionShareOrderSerps:
                        |          description: >-
                        |            A flag set to indicate whether the insured person is subject to a
                        |            Pension Share Order (SERPS); false - they are not, true - they are
                        |          type: boolean
                        |        reducedRateElectionToConsider:
                        |          description: >-
                        |            A flag set to indicate whether there was a Reduced Rate Election in
                        |            force at the beginning of the tax year 35 years before the tax year
                        |            of SPA; false - there was not, true - there was
                        |          type: boolean
                        |        sensitiveCaseFlag:
                        |          description: >-
                        |            A nationally set indicator on a computer account which indicates the
                        |            nature of sensitivity and inhibits access to the account except for
                        |            person(s) with the required level of authority: 0 - NON SENSITIVE, 1
                        |            - TRANSSEXUAL, 2 - VIP, 3 - OTHER, 9- SPECIAL MONITOR
                        |          type: integer
                        |          minimum: 0
                        |          maximum: 9
                        |        sex:
                        |          type: string
                        |          enum:
                        |            - M
                        |            - F
                        |            - U
                        |        spaDate:
                        |          description: The State Pension Age of the insured person. format(CCYY-MM-DD)
                        |          type: string
                        |          minLength: 10
                        |          maxLength: 10
                        |        pensionForecast:
                        |          type: object
                        |          properties:
                        |            forecastAmount:
                        |              description: Forecast weekly amount at State Pension Age
                        |              type: number
                        |              minimum: -9999999999999.99
                        |              maximum: 9999999999999.99
                        |              multipleOf: 0.01
                        |            forecastAmount2016:
                        |              type: number
                        |              minimum: -9999999999999.99
                        |              maximum:  9999999999999.99
                        |              multipleOf: 0.01
                        |            nspMax:
                        |              description: Maximum amount of New State Pension
                        |              type: number
                        |              minimum: -9999999999999.99
                        |              maximum:  9999999999999.99
                        |              multipleOf: 0.01
                        |            qualifyingYearsAtSpa:
                        |              description: >-
                        |                The number of qualifying years the person will have if they
                        |                continue to contribute at the current rate at State Pension Age
                        |              type: integer
                        |              minimum: 0
                        |              maximum: 99
                        |          additionalProperties: false
                        |        statePensionAmount:
                        |          type: object
                        |          properties:
                        |            apAmount:
                        |              description: >-
                        |                The amount of AP accrued by the person for the last fully posted
                        |                tax year.
                        |              type: number
                        |              minimum: -9999999999999.99
                        |              maximum:  9999999999999.99
                        |              multipleOf: 0.01
                        |            amountA2016:
                        |              description: Pre 2016 Amount A Pension Details
                        |              type: object
                        |              properties:
                        |                grbCash:
                        |                  description: The current cash value of the Graduated Retirement Benefit
                        |                  type: number
                        |                  minimum: -9999999999999.99
                        |                  maximum:  9999999999999.99
                        |                  multipleOf: 0.01
                        |                ltbCatACashValue:
                        |                  description: The pre 2016 Old Rules Basic Pension amount
                        |                  type: number
                        |                  minimum: -9999999999999.99
                        |                  maximum: 9999999999999.99
                        |                  multipleOf: 0.01
                        |                ltbPost02ApCashValue:
                        |                  description: >-
                        |                    The amount of additional pension accrued between 2002 and
                        |                    2016
                        |                  type: number
                        |                  minimum: -9999999999999.99
                        |                  maximum:  9999999999999.99
                        |                  multipleOf: 0.01
                        |                ltbPost88CodCashValue:
                        |                  description: >-
                        |                    The current cash value of the Contracted Out Deduction from
                        |                    1988 onwards
                        |                  type: number
                        |                  minimum: -9999999999999.99
                        |                  maximum:  9999999999999.99
                        |                  multipleOf: 0.01
                        |                ltbPost97ApCashValue:
                        |                  description: >-
                        |                    The amount of additional pension accrued between 1997 and
                        |                    2002
                        |                  type: number
                        |                  minimum:  -9999999999999.99
                        |                  maximum:  9999999999999.99
                        |                  multipleOf: 0.01
                        |                ltbPre88CodCashValue:
                        |                  description: >-
                        |                    The current cash value of the Contracted Out Deduction pre
                        |                    1988
                        |                  type: number
                        |                  minimum: -9999999999999.99
                        |                  maximum:  9999999999999.99
                        |                  multipleOf: 0.01
                        |                ltbPre97ApCashValue:
                        |                  description: >-
                        |                    The pre Pre-97 pension amount before the contracted out
                        |                    deduction has taken place
                        |                  type: number
                        |                  minimum: -9999999999999.99
                        |                  maximum:  9999999999999.99
                        |                  multipleOf: 0.01
                        |                ltbPst88GmpCashValue:
                        |                  description: >-
                        |                    The current cash value of the Guaranteed Minimum Pension
                        |                    from 1988 onwards
                        |                  type: number
                        |                  minimum: -9999999999999.99
                        |                  maximum:  9999999999999.99
                        |                  multipleOf: 0.01
                        |                pre88Gmp:
                        |                  description: The cash value of the Guaranteed Minimum Pension pre 1988
                        |                  type: number
                        |                  minimum: -9999999999999.99
                        |                  maximum:  9999999999999.99
                        |                  multipleOf: 0.01
                        |              additionalProperties: false
                        |            amountB2016:
                        |              description: Pre 2016 Amount B Pension Details
                        |              type: object
                        |              properties:
                        |                mainComponent:
                        |                  description: >-
                        |                    The New State Pension amount for the insured person at or
                        |                    before 2016 before the Rebate Derived Amount has been
                        |                    deducted
                        |                  type: number
                        |                  minimum: -9999999999999.99
                        |                  maximum:  9999999999999.99
                        |                  multipleOf: 0.01
                        |                rebateDerivedAmount:
                        |                  description: >-
                        |                    Adjustment to the New State Pension amount to take account
                        |                    of periods of contracting out
                        |                  type: number
                        |                  minimum: -9999999999999.99
                        |                  maximum:  9999999999999.99
                        |                  multipleOf: 0.01
                        |              additionalProperties: false
                        |            nspEntitlement:
                        |              description: >-
                        |                The current value of New State Pension for the insured person at
                        |                the time of calculation
                        |              type: number
                        |              minimum: -9999999999999.99
                        |              maximum:  9999999999999.99
                        |              multipleOf: 0.01
                        |            protectedPayment2016:
                        |              description: The Protected Payment for the insured person at or before 2016
                        |              type: number
                        |              minimum: -9999999999999.99
                        |              maximum:  9999999999999.99
                        |              multipleOf: 0.01
                        |            startingAmount:
                        |              description: The Starting Amount for the insured person at or before 2016
                        |              type: number
                        |              minimum: -9999999999999.99
                        |              maximum:  9999999999999.99
                        |              multipleOf: 0.01
                        |          additionalProperties: false
                        |      additionalProperties: false
                        |""".stripMargin
}
