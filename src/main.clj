(ns main
  (:require [server] [bus]))

(server/serve (bus/bus) 8888)
