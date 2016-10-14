# from numpy import cumsum, sort, sum, searchsorted
# from numpy.random import rand
# from pylab import hist,show,xticks

# def weighted_pick(weights,n_picks):
#  """
#   Weighted random selection
#   returns n_picks random indexes.
#   the chance to pick the index i 
#   is give by the weight weights[i].
#  """
#  t = cumsum(weights)
#  print t
#  s = sum(weights)
#  print s
#  return searchsorted(t,rand(n_picks)*s)

# # weights, don't have to sum up to one
# w = [0.1, 0.2, 0.5, 0.5, 1.0, 1.1, 2.0]

# # picking 10000 times
# picked_list = weighted_pick(w,20)

# print(picked_list)

# # hist(picked_list,bins=len(w),normed=1,alpha=.8,color='red')
# # show()
# print rand(20)*5.4


from random import random
from bisect import bisect

def weighted_choice(choices):
    values, weights = zip(*choices)
    total = 0
    cum_weights = []
    for w in weights:
        total += w
        cum_weights.append(total)
    x = random() * total
    i = bisect(cum_weights, x)
    return values[i]

for 
choice = weighted_choice([(1,50), (0.5,20), (2,20), (0.1,5), (5,5)])
print choice