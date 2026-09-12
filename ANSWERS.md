### 任务二、修复缺失预约的错误响应

`GET /rooms/room-101/bookings/booking-missing`

返回500可能的原因：

1. `roomRepository`可能查询失败，抛出的异常没有具体的处理器，由兜底的全局异常处理器进行处理
2. `bookingRepository`查询失败后抛出的可能是`RuntimeException`，被`@ExceptionHandler(Exception.class)`处理
3. 查询失败后抛出的`BookingNotFoundException`可能没有具体的处理器，由兜底的全局异常处理器进行处理
4. `BookingNotFoundException`对应的处理器HTTP状态码可能有误

**原因1验证**

在`requireRoom(roomId);`处下断点，进入Debug并发起请求。发现`roomRepository`查询成功，并没有抛出异常，可以排除。

**原因2验证**

在`bookingRepository.findByIdAndRoomId(bookingId, roomId)`处下断点，进入Debug并发起请求。
查询数据库后发现bookingId并不存在，booking的值为null，抛出的异常类型为`BookingNotFoundException`，而不是`RuntimeException`
，因此可以排除。

**原因3验证**

`bookingRepository.findByIdAndRoomId(bookingId, roomId)`查询失败后抛出的异常类型为`BookingNotFoundException`，
并且在`ApiExceptionHandler`中有对应的`@ExceptionHandler(BookingNotFoundException.class)`进行处理，可以排除。

**原因4验证**

在`@ExceptionHandler(BookingNotFoundException.class)`对应的处理器中，发现HTTP状态码是`HttpStatus.INTERNAL_SERVER_ERROR`(
500)，
而不是`HttpStatus.NOT_FOUND`(404)，可以确定是该原因导致。

在HTTP 4xx的状态码中，"400"通常代表发送的 HTTP 请求存在问题，"404"通常代表请求的资源未在服务端找到，
但"room-101"和"booking-missing"均是合法规范的参数，并不符合"400"的含义，且"booking-missing"在数据库中确实并不存在，所以"
404"更合适。
其他常见的4xx状态码还有：

* 401 Unauthorized ：未认证却请求需要认证之后才能访问的资源。
* 403 Forbidden ：直接拒绝 HTTP 请求，不处理。一般用来针对非法请求。

### 任务三、修复可用性判断故障

`GET /rooms/room-202/availability?start=2030-01-15T10:15:00&end=2030-01-15T10:45:00`

返回true的原因：

1. `candidateStartsBeforeExistingEnds`或`existingStartsBeforeCandidateEnds`的值可能存在计算错误，导致`hasConflict`结果不准确。
2. 可能是`!hasConflict`取反错误
3. 可能出现多次循环，导致原有结果被覆盖，由最后一次的结果决定。

区分：

* 原因1可以分别计算`candidateStartsBeforeExistingEnds`和`existingStartsBeforeCandidateEnds`，检查是否有计算错误
* 原因2可以计算`!hasConflict`是否不应该取反
* 原因3可以检查每次计算得到的`hasConflict`是否存在覆盖问题

验证：
在`boolean candidateStartsBeforeExistingEnds = start.isBefore(existing.end())`下断点，进入Debug发起HTTP请求，
第一轮循环结束后得到

```
existing = booking-2021
candidateStartsBeforeExistingEnds = true
existingStartsBeforeCandidateEnds = true
overlaps = true
hasConflict = true
```

确认并不存在计算错误及取反错误问题，原因1和2可以排除。进入第二轮循环，结束后得到

```
existing = booking-2022
candidateStartsBeforeExistingEnds = true
existingStartsBeforeCandidateEnds = false
overlaps = false
hasConflict = false
```

此时出现冲突，`hasConflict`的值从true变为了false，第一轮的值被覆盖，最后返回的是true。
可以确定就是原因3导致出现可用性判断故障。

#### 假设线上系统使用关系型数据库保存预约数据，某个 room 已有数十亿条 booking，当前实现可能会有什么问题？你会如何优化？

**问题：**
当数据库中某个room的数据量非常庞大时，当前实现会一条一条的进行查询，查询集过大，导致消耗的时间、资源较多，
而且只需要查询到一条有冲突的记录即可，不需要全都扫描一遍。

**优化**
建立roomId索引，可以直接定位到具体的roomId，跳过其他无关的记录。然后只查询一条记录即可，也就是`select 1 from xxx`，
在where语句中添加上`start < ? and end > ?`，过滤出符合条件的记录。

### 任务四、实现创建预约接口

"沿 Location 响应头查询新资源"。我使用的是ApiFox进行的接口请求测试，可以正常查询并返回响应。

**语义**

POST通常用于创建/提交新的资源，
PUT通常用于全量更新整个资源，
而PATCH则是局部更新资源。

在本任务中是向room中的booking集合中创建一个新的booking，符合POST的语义，创建并提交一个新的资源。

**幂等性**

幂等是指对同一个资源重复执行相同请求，多次执行后的最终服务器状态应该和执行一次相同。
PUT是幂等方法，因为PUT是将完整的最终状态重复写了多遍，但始终数据始终不变，
PATCH只是修改状态时是幂等的，但由于支持`increment`、`add`等操作也可以非幂等。
POST则是每次都有可能新增一个资源，所以是非幂等的。

**其他常见的HTTP方法**

GET：查询资源
DELETE：删除资源
HEAD：和 GET 类似，但是通常不返回响应体，只获取响应头