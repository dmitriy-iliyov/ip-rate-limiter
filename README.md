[![codecov](https://codecov.io/gh/dmitriy-iliyov/ip-rate-limiter/graph/badge.svg?token=9YJ5LQ45XF)](https://codecov.io/gh/dmitriy-iliyov/ip-rate-limiter)
[![CI](https://github.com/dmitriy-iliyov/ip-rate-limiter/actions/workflows/ci.yml/badge.svg)](https://github.com/dmitriy-iliyov/ip-rate-limiter/actions/workflows/ci.yml)

## Overview
A Spring Security IP-based request rate limiting filter using Redis and Lua scripting for atomic counter operations. 

Each incoming request to a configured endpoint is checked against a Redis counter for the client's IP.  
The counter increments atomically via a Lua script — if the count exceeds the limit within the observe window, the IP is blocked for a configurable duration and all further requests immediately receive `429 Too Many Requests`.

## Gatling Test
