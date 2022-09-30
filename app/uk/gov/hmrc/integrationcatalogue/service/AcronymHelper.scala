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

/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.integrationcatalogue.service

trait AcronymHelper {

  val acronymLookupMap = Map(
    "BBSI"           -> "Bank and Building Society Interest",
    "BVD"            -> "Birth Verification Data",
    "CASEFLOW"       -> "Caseflow",
    "CESA"           -> "Computerised Environment for Self-Assessment",
    "CID"            -> "Citizen Information Database",
    "CISR"           -> "Common Ingestion Service Reform",
    "COBRA2"         -> "Cash Operational Banking Reconciliation Activity",
    "CompaniesHouse" -> "Companies House",
    "DMS"            -> "Digital Mail Service",
    "DPS"            -> "Data Provisioning Systems",
    "DS"             -> "Decision Service",
    "DTR"            -> "Database of Trader Registration",
    "EDH"            -> "Enterprise Data Hub",
    "EDM"            -> "EDM Group – Scanning supplier",
    "ETMP"           -> "Enterprise Tax Management Platform",
    "FIM"            -> "Forefront Identity Management",
    "FWKS"           -> "The Frameworks System",
    "INTDS"          -> "Barclays ",
    "ITMP"           -> "Individual Tax Management Platform",
    "ITSD"           -> "Income Tax Sub Domain",
    "MDTP"           -> "Multi-channel Digital Tax Platform",
    "NPS"            -> "National Insurance and PAYE System",
    "RDCO"           -> "Registered Dealers of Controlled Oil",
    "RTI"            -> "Realtime Information",
    "ServiceNow"     -> "Service Now",
    "SWORD"          -> "Suspect Warehouse of Risk Data",
    "VDB"            -> "VAT Database",
    "VIES"           -> "VAT Information Exchange System",
    "VMF"            -> "VAT Mainframe"
  )

}
