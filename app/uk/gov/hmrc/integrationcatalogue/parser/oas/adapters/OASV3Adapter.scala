/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.integrationcatalogue.parser.oas.adapters

import cats.data.Validated._
import cats.data._
import cats.implicits._
import com.fasterxml.jackson.databind.JsonNode
import io.swagger.v3.oas.models.PathItem.HttpMethod
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.security.{SecurityRequirement, SecurityScheme}
import io.swagger.v3.oas.models.{OpenAPI, Operation, PathItem}
import play.api.Logging
import uk.gov.hmrc.integrationcatalogue.config.AppConfig
import uk.gov.hmrc.integrationcatalogue.models._
import uk.gov.hmrc.integrationcatalogue.models.common._
import uk.gov.hmrc.integrationcatalogue.parser.oas.OASV3Validation
import uk.gov.hmrc.integrationcatalogue.service.{AcronymHelper, UuidService}

import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.collection.mutable
import scala.jdk.CollectionConverters._

@Singleton
class OASV3Adapter @Inject() (uuidService: UuidService, appConfig: AppConfig)
  extends Logging with AcronymHelper with OASV3Validation with OASExtensionsAdapter {

  def extractOpenApi(
                      publisherRef: Option[String],
                      platformType: PlatformType,
                      specType: SpecificationType,
                      openApi: OpenAPI,
                      openApiSpecificationContent: String
                    ): ValidatedNel[List[String], ApiDetail] = {

    Option(openApi.getInfo) match {
      case Some(info) =>
        validateInfo(info) match {
          case Invalid(errors) => errors.toList.invalidNel[ApiDetail]
          case Valid(_)        =>
            val globalScopes = extractScopes(openApi, openApi.getSecurity)
            val mayBePaths   = Option(openApi.getPaths)
            val pathNames    = mayBePaths.map(_.keySet().asScala.to(mutable.LinkedHashSet).toList).getOrElse(List.empty)
            val allEndpoints = pathNames.flatMap(pathName => {
              mayBePaths.map(path => {
                val pathItem = path.get(pathName)
                extractEndpoints(openApi, pathName, pathItem, globalScopes)
              }).getOrElse(List.empty)
            })

            val allScopeDefinitions = buildScopeDefinitions(allEndpoints, openApi)

            parseExtensions(info, publisherRef, appConfig) match {
              case Right(extensions: IntegrationCatalogueExtensions) =>
                Valid(ApiDetail(
                  id = IntegrationId(uuidService.newUuid()),
                  publisherReference = extensions.publisherReference,
                  title = getStringSafe(info.getTitle),
                  description = getStringSafe(info.getDescription),
                  version = getStringSafe(info.getVersion),
                  lastUpdated = Instant.now,
                  endpoints = allEndpoints,
                  maintainer = extractMaintainer(info.getContact),
                  specificationType = specType,
                  platform = platformType,
                  hods = extensions.backends.toList,
                  shortDescription = extensions.shortDescription,
                  openApiSpecification = openApiSpecificationContent,
                  apiStatus = extensions.status,
                  reviewedDate = extensions.reviewedDate,
                  scopes = allScopeDefinitions,
                  domain = extensions.domain,
                  subDomain = extensions.subDomain
                ))
              case Left(x)                                           => x.toList.invalidNel[ApiDetail]
            }
        }
      case None       => List("Invalid OAS, info item missing from OAS specification").invalidNel[ApiDetail]
    }
  }

  private def buildScopeDefinitions(allEndpoints: List[Endpoint], openApi: OpenAPI): Set[Scope] = {
    val scopeNamesInUseByEndpoints = allEndpoints.flatMap(e => e.methods).flatMap(m => m.scopes).toSet
    val securityScopes = extractSecurityScopes(openApi)
    val securityScopesInUse = securityScopes.filter(scope => scopeNamesInUseByEndpoints.contains(scope.name))
    val securityScopeNamesInUse = securityScopesInUse.map(scope => scope.name)
    val scopeNamesUsedButWithoutDefinition = scopeNamesInUseByEndpoints -- securityScopeNamesInUse
    val scopesUsedButWithoutDefinition = scopeNamesUsedButWithoutDefinition.map(name => Scope(name, Option.empty))
    val allScopeDefinitions = securityScopesInUse ++ scopesUsedButWithoutDefinition
    allScopeDefinitions
  }
  private def getStringSafe(value: java.lang.String): String = {
    Option(value).getOrElse("")
  }

  private def extractMaintainer(contact: Contact) =
    Maintainer(name = "", slackChannel = "", contactInfo = extractContact(contact).map(List(_)).getOrElse(List.empty))

  private def extractContact(contact: Contact): Option[ContactInformation] = {
    Option(contact).map(x => ContactInformation(handleNullAsString(Option(x.getName)), handleNullAsString(Option(x.getEmail))))
  }

  private def handleNullAsString(value: Option[String]) = {
    value match {
      case Some("null") => None
      case _            => value
    }
  }

  private def extractEndpoints(openApi: OpenAPI, path: String, item: PathItem, globalScopes: List[String]): List[Endpoint] = {
    val endpointMethods = item.readOperationsMap().asScala.toMap
      .map {
        case (m: HttpMethod, operation: Operation) =>
          val method              = Option(m).map(_.toString).getOrElse("")

          EndpointMethod(
            httpMethod = method,
            summary = Option(operation).flatMap(x => Option(x.getSummary)),
            description = Option(operation.getDescription),
            scopes = extractScopes(openApi, operation.getSecurity, globalScopes)
          )
      }.toList
    List(Endpoint(path, endpointMethods))
  }

  private def extractScopes(openApi: OpenAPI, securityRequirements: java.util.List[SecurityRequirement]): List[String] = {
    (for {
      schemeName <- extractOAuth2SchemeName(openApi)
      requirements <- Option(securityRequirements)
    } yield {
      requirements.asScala.toList
        .flatMap(
          securityRequirement =>
            Option(securityRequirement.get(schemeName))
              .map(_.asScala.toList)
              .getOrElse(List.empty)
        )
    }).getOrElse(List.empty)
  }

  private def extractScopes(openApi: OpenAPI, securityRequirements: java.util.List[SecurityRequirement], globalScopes: List[String]): List[String] = {
    extractScopes(openApi, securityRequirements) match {
      case scopes @ _ :: _  => scopes
      case _ => globalScopes
    }
  }

  private def extractOAuth2SchemeName(openApi: OpenAPI): Option[String] = {
    (for {
      components <- Option(openApi.getComponents)
      securitySchemes <- Option(components.getSecuritySchemes)
    } yield securitySchemes.asScala)
      .flatMap(
        _.find(scheme => scheme._2.getType.equals(SecurityScheme.Type.OAUTH2))
          .map(_._1)
      )
  }

  private def extractSecurityScopes(openApi: OpenAPI) : Set[Scope] = {

    val components = Option(openApi.getComponents)
    val securitySchemes = components.map(c => c.getSecuritySchemes)
    val oauth2SchemeName = extractOAuth2SchemeName(openApi)
    val securityScheme = securitySchemes.flatMap(ss => oauth2SchemeName.map(oasn => ss.get(oasn)))
    val flows = securityScheme.map(ss => ss.getFlows)

    val authorizationCodeFlowScopes = flows.flatMap(f => Option(f.getAuthorizationCode)).map(flow => flow.getScopes.asScala).getOrElse(Map.empty)
    val implicitFlowScopes = flows.flatMap(f => Option(f.getImplicit)).map(flow => flow.getScopes.asScala).getOrElse(Map.empty)
    val passwordFlowScopes = flows.flatMap(f => Option(f.getPassword)).map(flow => flow.getScopes.asScala).getOrElse(Map.empty)
    val clientCredentialsFlowScopes = flows.flatMap(f => Option(f.getClientCredentials)).map(flow => flow.getScopes.asScala).getOrElse(Map.empty)

    val allScopes = authorizationCodeFlowScopes ++ implicitFlowScopes ++ passwordFlowScopes ++ clientCredentialsFlowScopes

    allScopes.toSet[(String, String)].map(kv => Scope(kv._1, Option.apply(kv._2)))
  }

  def getExampleText(maybeObject: Option[Object]): String = {
    maybeObject.map { (o: Object) =>
    {
      o match {
        case js: JsonNode => js.toPrettyString
        case x: Object    => x.toString
      }
    }
    }.getOrElse("")
  }

}
