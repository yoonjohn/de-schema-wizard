# Define a job called my-service
job "schema-wizard" {
    region = "global"

    datacenters = ["dc1"]

    type = "service"

    # Rolling updates should be sequential
    update {
        stagger = "30s"
        max_parallel = 1
    }

    group "schema-wizard" {
        count = 1

        
        task "shared-volume" {
            driver = "raw_exec"
            config {
                command = "docker"
                args = ["create", "--name", "shared-volume", "-v", "/datavolume:/usr/local/shared/untrusted", "python:3.5.1", "/bin/true"]
            }
		   resources {
                cpu = 20
                memory = 20
           } 
        }
        
        
        task "r-cache" {
            driver = "docker"
            config {
                image = "redis:3.2"
            }
            service {
                port = "redis"
                check {
                    type = "tcp"
                    interval = "10s"
                    timeout = "2s"
                }
            }
            resources {
                cpu = 500
                memory = 128
                network {
                    mbits = 2
                    port "redis" {
                        static = 6379
                    }
                }
            }
        }
        
        task "sw-h2" {
            driver = "docker"
            config {
                image = "deleidos/de-schema-wizard-h2:3.0.0-beta1"

            }
            service {
                port = "h2"
                check {
                    type = "tcp"
                    interval = "10s"
                    timeout = "2s"
                }
            }
            env {
                H2_DB_DIR = "/usr/local/h2"
				H2_DB_NAME = "data"
            }
            resources {
                cpu = 500
                memory = 128
                network {
                    mbits = 2
                    port "h2" {
                        static = 9123
                    }
                }
            }
        }
        
        
        task "sw-mongodb" {
            driver = "docker"
            config {
                image = "deleidos/de-schema-wizard-mongodb:3.0.0-beta1"
                
            }
            service {
                port = "mongo"
                check {
                    type = "tcp"
                    interval = "10s"
                    timeout = "2s"
                }
            }
            env {
                H2_DB_DIR = "/usr/local/h2"
				H2_DB_NAME = "data"
            }
            resources {
                cpu = 500
                memory = 128
                network {
                    mbits = 2
                    port "mongo" {
                        static = 27017
                    }
                }
            }
        }
        
        
        
        task "sw-ie-sidekick" {
            driver = "docker"
            config {
                image = "deleidos/de-schema-wizard-sidekick:3.0.0-beta1"
                
            }
            env {
                U_PROFILE = "docker-untrusted"
				U_IMAGE = "deleidos/de-schema-wizard-untrusted:3.0.0-beta1"
				PULL_TAG = "142"
				REDIS_PORT = "tcp://r-cache:6379"
            }
            resources {
                cpu = 100
                memory = 32
                network {
                    mbits = 2
                }
            }
        }
        
        
        
        
        
        
        
        
        
        
        
        task "sw-webapp" {
            driver = "docker"
            config {
                image = "deleidos/de-schema-wizard-webapp:3.0.0-beta1"

            }
            service {
                port = "webapp"
                check {
                    type = "tcp"
                    interval = "10s"
                    timeout = "2s"
                }
            }
            env {
                H2_DB_DIR = "/usr/local/h2"
				H2_DB_NAME = "data"
				H2_DB_PORT = "tcp://sw-h2:9123"
				SW_IE_PORT = "tcp://sw-ie:5000"
            }
            resources {
                cpu = 500
                memory = 2048
                network {
                    mbits = 2
                    port "webapp" {
                        static = 8080
                    }
                }
            }
        }
        
        
        
        
        
        
        
        
    }
}
