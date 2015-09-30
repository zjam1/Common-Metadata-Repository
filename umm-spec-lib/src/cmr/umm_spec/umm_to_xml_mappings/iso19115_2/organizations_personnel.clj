(ns cmr.umm-spec.umm-to-xml-mappings.iso19115-2.organizations-personnel
  "Functions for generating ISO19115-2 XML elements from UMM organization and personnel records."
  (:require [cmr.umm-spec.xml.gen :refer :all]
            [clojure.string :as str]
            [cmr.umm-spec.iso19115-2-util :as iso]
            [cmr.umm-spec.iso-utils :as iso-utils]))


(defn responsibility-by-role
  [responsibilities role]
  (filter (fn [responsibility]
            (= (:Role responsibility) role)) responsibilities))

(defn- contact-values-by-type
  [contacts type]
  (map :Value (filter #(= (:Type %)
                          (str/lower-case type)) contacts)))

(defn generate-online-resource-url
  "Returns content generator instructions for an online resource url or access url"
  [online-resource-url]
  (let [{:keys [URLs Description]} online-resource-url]
    (for [url URLs]
      [:gmd:onlineResource
       [:gmd:CI_OnlineResource
        [:gmd:linkage
         [:gmd:URL url]]
        (if Description
          [:gmd:description
           (char-string Description)]
          [:gmd:description {:gco:nilReason "missing"}])
        ;; Online Resource url is hardcoded to information since the url
        ;; corresponds to information about the party
        [:gmd:function
         [:gmd:CI_OnLineFunctionCode
          {:codeList (str (:ngdc iso/code-lists) "#CI_OnLineFunctionCode")
           :codeListValue "information"} "information"]]]])))

(defn generate-responsible-party
  [responsibility]
  (let [{:keys [Role] {:keys [OrganizationName Person ServiceHours
                              ContactInstructions Contacts Addresses
                              RelatedUrls]} :Party} responsibility
        role-code (str/lower-case Role)]
    [:gmd:CI_ResponsibleParty
     (when-let [{:keys [FirstName MiddleName LastName]} Person]
       [:gmd:individualName (char-string
                              (str/join
                                " " (remove nil? [FirstName MiddleName LastName])))])
     [:gmd:organisationName (char-string (:ShortName OrganizationName))]
     [:gmd:positionName {:gco:nilReason "missing"}]
     [:gmd:contactInfo
      [:gmd:CI_Contact
       (for [phone (contact-values-by-type Contacts "phone")]
         [:gmd:phone
          [:gmd:CI_Telephone
           [:gmd:voice (char-string phone)]]])
       (for [address Addresses
             :let [{:keys [StreetAddresses City StateProvince PostalCode Country]} address]]
         [:gmd:address
          [:gmd:CI_Address
           (for [street-address StreetAddresses]
             [:gmd:deliveryPoint (char-string street-address)])
           [:gmd:city (char-string City)]
           [:gmd:administrativeArea (char-string StateProvince)]
           [:gmd:postalCode (char-string PostalCode)]
           [:gmd:country (char-string Country)]
           (for [email (contact-values-by-type Contacts "email")]
             [:gmd:electronicMailAddress (char-string email)])]])
       [:gmd:onlineResource ]
       (for [online-resource-url RelatedUrls]
         (generate-online-resource-url online-resource-url))
       [:gmd:ServiceHours ServiceHours]
       [:gmd:contactInstructions ContactInstructions]]]
     [:gmd:role
      [:gmd:CI_RoleCode {:codeList (str (:ngdc iso/code-lists) "#CI_RoleCode")
                         :codeListValue role-code} role-code]]]))