(ns pallet.crate.ssh-key-test
  (:use pallet.crate.ssh-key)
  (:require
   [pallet.action :as action]
   [pallet.action.directory :as directory]
   [pallet.action.exec-script :as exec-script]
   [pallet.action.file :as file]
   [pallet.action.remote-file :as remote-file]
   [pallet.action.user :as user]
   [pallet.build-actions :as build-actions]
   [pallet.core :as core]
   [pallet.context :as context]
   [pallet.live-test :as live-test]
   [pallet.parameter :as parameter]
   [pallet.phase :as phase]
   [pallet.script.lib :as lib]
   [pallet.stevedore :as stevedore]
   [pallet.template :as template]
   [pallet.utils :as utils]
   [clojure.contrib.logging :as logging]
   [clojure.string :as string])
  (:use clojure.test
        pallet.test-utils))

(use-fixtures :once with-ubuntu-script-template)


(deftest authorize-key-test
  (is (= (first
          (context/with-phase-context
           :authorize-key "Authorize SSH key on user fred"
           (build-actions/build-actions
            {}
            (directory/directory
             "$(getent passwd fred | cut -d: -f6)/.ssh/"
             :owner "fred" :mode "755")
            (file/file
             "$(getent passwd fred | cut -d: -f6)/.ssh/authorized_keys"
             :owner "fred" :mode "644")
            (exec-script/exec-checked-script
             "authorize-key on user fred"
             (var auth_file
                  "$(getent passwd fred | cut -d: -f6)/.ssh/authorized_keys")
             (if-not (fgrep (quoted "key1") @auth_file)
               (echo (quoted "key1") ">>" @auth_file)))
            (exec-script/exec-checked-script
             "Set selinux permissions"
             (~lib/selinux-file-type
              "$(getent passwd fred | cut -d: -f6)/.ssh/" "user_home_t")))))
         (first
          (build-actions/build-actions
           {}
           (authorize-key "fred" "key1"))))))

(deftest install-key-test
  (is (= (first
          (build-actions/build-actions
           {}
           (directory/directory
            "$(getent passwd fred | cut -d: -f6)/.ssh/"
            :owner "fred" :mode "755")
           (remote-file/remote-file
            "$(getent passwd fred | cut -d: -f6)/.ssh/id"
            :content "private" :owner "fred" :mode "600")
           (remote-file/remote-file
            "$(getent passwd fred | cut -d: -f6)/.ssh/id.pub"
            :content "public" :owner "fred" :mode "644")))
         (first
          (build-actions/build-actions
           {} (install-key "fred" "id" "private" "public")))))
  (is (= (first
          (build-actions/build-actions
           {}
           (directory/directory
            "$(getent passwd fred | cut -d: -f6)/.ssh/"
            :owner "fred" :mode "755")
           (remote-file/remote-file
            "$(getent passwd fred | cut -d: -f6)/.ssh/id"
            :content "private" :owner "fred" :mode "600")
           (remote-file/remote-file
            "$(getent passwd fred | cut -d: -f6)/.ssh/id.pub"
            :content "public" :owner "fred" :mode "644")))
         (first
          (build-actions/build-actions
           {}
           (install-key "fred" "id" "private" "public"))))))

(deftest generate-key-test
  (is (= (first
          (build-actions/build-actions
           {}
           (directory/directory
            "$(getent passwd fred | cut -d: -f6)/.ssh"
            :owner "fred" :mode "755")
           (exec-script/exec-checked-script
            "ssh-keygen"
            (var key_path "$(getent passwd fred | cut -d: -f6)/.ssh/id_rsa")
            (if-not (file-exists? @key_path)
              (ssh-keygen
               ~(stevedore/map-to-arg-string
                 {:f (stevedore/script @key_path) :t "rsa" :N ""
                  :C "generated by pallet"}))))
           (file/file
            "$(getent passwd fred | cut -d: -f6)/.ssh/id_rsa"
            :owner "fred" :mode "0600")
           (file/file
            "$(getent passwd fred | cut -d: -f6)/.ssh/id_rsa.pub"
            :owner "fred" :mode "0644")))
         (first
          (build-actions/build-actions
           {}
           (generate-key "fred")))))

  (is (= (first
          (build-actions/build-actions
           {}
           (directory/directory
            "$(getent passwd fred | cut -d: -f6)/.ssh"
            :owner "fred" :mode "755")
           (exec-script/exec-checked-script
            "ssh-keygen"
            (var key_path "$(getent passwd fred | cut -d: -f6)/.ssh/id_dsa")
            (if-not (file-exists? @key_path)
              (ssh-keygen
               ~(stevedore/map-to-arg-string
                 {:f (stevedore/script @key_path) :t "dsa" :N ""
                  :C "generated by pallet"}))))
           (file/file
            "$(getent passwd fred | cut -d: -f6)/.ssh/id_dsa"
            :owner "fred" :mode "0600")
           (file/file
            "$(getent passwd fred | cut -d: -f6)/.ssh/id_dsa.pub"
            :owner "fred" :mode "0644")))
         (first
          (build-actions/build-actions
           {} (generate-key "fred" :type "dsa")))))

  (is (= (first
          (build-actions/build-actions
           {}
           (directory/directory
            "$(getent passwd fred | cut -d: -f6)/.ssh"
            :owner "fred" :mode "755")
           (exec-script/exec-checked-script
            "ssh-keygen"
            (var key_path "$(getent passwd fred | cut -d: -f6)/.ssh/identity")
            (if-not (file-exists? @key_path)
              (ssh-keygen
               ~(stevedore/map-to-arg-string
                 {:f (stevedore/script @key_path) :t "rsa1" :N ""
                  :C "generated by pallet"}))))
           (file/file
            "$(getent passwd fred | cut -d: -f6)/.ssh/identity"
            :owner "fred" :mode "0600")
           (file/file
            "$(getent passwd fred | cut -d: -f6)/.ssh/identity.pub"
            :owner "fred" :mode "0644")))
         (first
          (build-actions/build-actions
           {} (generate-key "fred" :type "rsa1")))))

  (is (= (first
          (build-actions/build-actions
           {}
           (exec-script/exec-checked-script
            "ssh-keygen"
            (var key_path "$(getent passwd fred | cut -d: -f6)/.ssh/c")
            (if-not (file-exists? @key_path)
              (ssh-keygen
               ~(stevedore/map-to-arg-string
                 {:f (stevedore/script @key_path)
                  :t "rsa1" :N "abc"  :C "my comment"}))))
           (file/file "$(getent passwd fred | cut -d: -f6)/.ssh/c"
                      :owner "fred" :mode "0600")
           (file/file "$(getent passwd fred | cut -d: -f6)/.ssh/c.pub"
                      :owner "fred" :mode "0644")))
         (first
          (build-actions/build-actions
           {}
           (generate-key
            "fred" :type "rsa1" :file "c" :no-dir true
            :comment "my comment" :passphrase "abc"))))))

(deftest authorize-key-for-localhost-test
  (is (= (first
          (build-actions/build-actions
           {}
           (directory/directory
            "$(getent passwd fred | cut -d: -f6)/.ssh/"
            :owner "fred" :mode "755")
           (file/file
            "$(getent passwd fred | cut -d: -f6)/.ssh/authorized_keys"
            :owner "fred" :mode "644")
           (exec-script/exec-checked-script
            "authorize-key"
            (var key_file "$(getent passwd fred | cut -d: -f6)/.ssh/id_dsa.pub")
            (var auth_file
                 "$(getent passwd fred | cut -d: -f6)/.ssh/authorized_keys")
            (if-not (grep (quoted @(cat @key_file)) @auth_file)
              (do
                (echo -n (quoted "from=\\\"localhost\\\" ") ">>" @auth_file)
                (cat @key_file ">>" @auth_file))))))
         (first
          (build-actions/build-actions
           {}
           (authorize-key-for-localhost "fred" "id_dsa.pub")))))

  (is (= (first
          (build-actions/build-actions
           {}
           (directory/directory
            "$(getent passwd tom | cut -d: -f6)/.ssh/"
            :owner "tom" :mode "755")
           (file/file
            "$(getent passwd tom | cut -d: -f6)/.ssh/authorized_keys"
            :owner "tom" :mode "644")
           (exec-script/exec-checked-script
            "authorize-key"
            (var key_file "$(getent passwd fred | cut -d: -f6)/.ssh/id_dsa.pub")
            (var auth_file
                 "$(getent passwd tom | cut -d: -f6)/.ssh/authorized_keys")
            (if-not (grep (quoted @(cat @key_file)) @auth_file)
              (do
                (echo -n (quoted "from=\\\"localhost\\\" ") ">>" @auth_file)
                (cat @key_file ">>" @auth_file))))))
         (first
          (build-actions/build-actions
           {}
           (authorize-key-for-localhost
            "fred" "id_dsa.pub" :authorize-for-user "tom"))))))

(deftest invoke-test
  (is (build-actions/build-actions
       {}
       (authorize-key "user" "pk")
       (authorize-key-for-localhost "user" "pk")
       (install-key "user" "name" "pk" "pubk")
       (generate-key "user"))))

(defn check-public-key
  [request]
  (logging/trace (format "check-public-key request is %s" request))
  (is (string?
       (parameter/get-for-target request [:user :testuser :id_rsa])))
  request)

(deftest live-test
  (live-test/test-for
   [image live-test/*images*]
   ;; required dynamically, to prevent cyclical dependency
   (require '[pallet.crate.automated-admin-user :as automated-admin-user])
   (let [automated-admin-user
         (var-get
          (resolve 'pallet.crate.automated-admin-user/automated-admin-user))]
     (live-test/test-nodes
      [compute node-map node-types]
      {:ssh-key
       {:image image
        :count 1
        :phases
        {:bootstrap (phase/phase-fn
                     (automated-admin-user)
                     (user/user "testuser"))
         :configure (phase/phase-fn (generate-key "testuser"))
         :verify1 (phase/phase-fn
                   (record-public-key "testuser"))
         :verify2 (phase/phase-fn
                   (check-public-key))}}}
      (core/lift (:ssh-key node-types)
                 :phase [:verify1 :verify2]
                 :compute compute)))))
