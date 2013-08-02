Strategy by Isaac Supeene for the 2013 MANIAC Challenge.

/* ******** *\
 * OVERVIEW *
\* ******** */

The main strategy class Strategy, which implements the ManiacStrategyInterface, is a relatively simple class which delegates most computations to the Brain.

Each time new information arrives in the Strategy, it forwards it to the Brain, who in turn delegates it to one of his Agents.  Given that all this information has been received by the Brain, the Strategy relies on the Brain to have computed good return values for bids and auction parameters when they are needed.

/* Agents *\

Each Agent is essentially a Message Pump implementing the Command Pattern, and (at least in theory) has a set of clearly defined responsibilities which fit into the overall strategy.  Their names and public methods should provide a good indication of what they're there for.  There is often a great deal of interdependency between agents however, particularly the History Agent and the Topology Agent, which are entangled in a neverending bond of true confusion.

/* The Formula *\

The AuctionAgent, when calculating some auction parameters, first looks for special Cases it can take advantage of, and then calls the method implemented by that Case to come up with the parameters (Case is an abstract class within AuctionAgent).  The normal case is home to some serious closures and function-oriented programming, which can be confusing at first, but try to read it as if it was a mathematical derivation or something.

/* The Algorithm *\

The 3D fibonacci-max algorithm is a three-dimensional extension of the fibonacci algorithm, which itself is an adaptation of the Golden Section search.  Google 'numerical optimization' or 'golden section search' or 'fibonacci search unimodal' for more information.

/* The API *\

The MANIAC API has been subtly but harmlessly altered to avoid some bugs and crashes (for example, changing the main loop in the Mothership class to 'while (!isInterrupted())')

/* ******* *\
 * CONTACT *
\* ******* */

If you have questions about the code that you would like answered, I can be contacted at isupeene {at} ualberta {dot} ca.  In particular, some of the code is (perhaps needlessly) complex and hard to get into - I can provide descriptions or explanations of certain pieces of code on request.  I would nonetheless advise you to choose a simpler approach to the problem, and be selective in the aspects you take from my strategy.
