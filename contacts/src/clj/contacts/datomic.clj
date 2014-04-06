(ns contacts.datomic
  (:require [datomic.api :as d]))





(defn list-contacts [db]
  (map
    ;; won't roundtrip to conn bc segment already probably cached
    #(d/entity db (first %))
    (d/q '[:find ?eid
          :where
          ;; talk about how we can make it do first OR last name
          [?eid :person/first-name]]
        db)))

(defn display-contacts [db]
  (let [contacts (list-contacts db)]
    (map
      #(select-keys % [:person/last-name :person/first-name])
      (sort-by :person/last-name contacts))))


(defn get-person [db])


;; return datoms to add
(defn add-person-datoms [db data])



(defn remove-person-datoms [db data])

(def initial-data
  (let [person-id (d/tempid :db.part/user)
        address-id (d/tempid :db.part/user)
        phone-id (d/tempid :db.part/user)
        email-id (d/tempid :db.part/user)]
    [{:db/id person-id
      :person/first-name "Bob"
      :person/last-name  "Smith"
      :person/email      email-id
      :person/telephone phone-id
      :person/address address-id}
     {:db/id email-id
      :email/address     "bob.smith@gmail.com"}
     {:db/id  phone-id
      :telephone/number "123-456-7890"}
     {:db/id address-id
      :address/street "111 Main St"
      :address/city "Brooklyn"
      :address/state "NY"
      :address/zipcode "11234"}]))

;; add/remove person, telephone, address, email
