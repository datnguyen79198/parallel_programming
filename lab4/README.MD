# Command and results

1) Create webserver in localhost:8080. Build and run java project through Maven.

```sh
> mvn package
> mvn exec:java
```

Result:

![alt text](https://github.com/datnguyen79198/parallel_programming/blob/main/lab4/cache/github1.PNG)

2) Testing REST api with server are currently running. I'm using extension to do it. First send POST request, and server will return message like this.

![alt text](https://github.com/datnguyen79198/parallel_programming/blob/main/lab4/cache/github2.PNG)

3) After received request from POST, server will response by routing to `packageId = {id}`

![alt text](https://github.com/datnguyen79198/parallel_programming/blob/main/lab4/cache/github3.PNG)
