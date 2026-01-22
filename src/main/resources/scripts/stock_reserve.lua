-- 재고 가점유 (Soft Reservation)
-- KEYS[1]: 재고 키 (stock:product:{id} 또는 stock:variant:{id})
-- ARGV[1]: 차감할 수량
-- 반환: 1(성공), 0(재고 부족), -1(키 없음)

local stock = redis.call('GET', KEYS[1])

if stock == false then
    return -1
end

local current = tonumber(stock)
local quantity = tonumber(ARGV[1])

if current >= quantity then
    redis.call('DECRBY', KEYS[1], quantity)
    return 1
else
    return 0
end
